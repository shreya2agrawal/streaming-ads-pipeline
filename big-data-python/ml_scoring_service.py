import pandas as pd
import numpy as np
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sqlalchemy import create_engine, text
from sklearn.ensemble import GradientBoostingClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import accuracy_score, classification_report
import joblib
import logging
import os
from dotenv import load_dotenv
from typing import Optional
import uvicorn

load_dotenv()

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="Netflix Ads CTR Prediction Service",
    description="Predicts BID_HIGH, BID_NORMAL, or BID_LOW for ad inventory slots",
    version="1.0.0"
)

DB_URL = os.getenv(
    'DATABASE_URL',
    'postgresql://ads_user:ads_pass@localhost:5433/adsplatform'
)

MODEL_PATH = 'model.pkl'
ENCODER_PATH = 'label_encoder.pkl'

FEATURES = [
    'banner_position',
    'device_type',
    'connection_type',
    'audience_segment_1',
    'audience_segment_2',
    'audience_segment_3',
]

model = None
label_encoder = None

class PredictRequest(BaseModel):
    show_id: str
    banner_position: int
    device_type: int
    connection_type: Optional[int] = 0
    audience_segment_1: Optional[int] = 0
    audience_segment_2: Optional[int] = 0
    audience_segment_3: Optional[int] = 0

class PredictResponse(BaseModel):
    show_id: str
    bid_signal: str
    ctr_probability: str
    confidence: str

class RetrainResponse(BaseModel):
    status: str
    rows_trained_on: int
    accuracy: float
    message: str

def get_engine():
    return create_engine(DB_URL)

def ctr_to_bid_signal(ctr_probability: float) -> str:
    if ctr_probability >= 0.08:
        return 'BID_HIGH'
    elif ctr_probability >= 0.04:
        return 'BID_NORMAL'
    else:
        return 'BID_LOW'
    
def ctr_to_confidence(ctr_probability: float) -> str:
    if ctr_probability >= 0.10 or ctr_probability <= 0.02:
        return 'HIGH'
    elif ctr_probability >= 0.06 or ctr_probability <= 0.035:
        return 'MEDIUM'
    else:
        return 'LOW'
    
def load_model():
    global model, label_encoder
    if os.path.exists(MODEL_PATH) and os.path.exists(ENCODER_PATH):
        model = joblib.load(MODEL_PATH)
        label_encoder = joblib.load(ENCODER_PATH)
        logger.info("Model loaded from disk")
        return True
    return False

def train_model():
    global model, label_encoder
    logger.info("Loading training data from PostgreSQL...")
    engine = get_engine()
    query = """
    SELECT 
        banner_position,
        device_type,
        connection_type,
        audience_segment_1,
        audience_segment_2,
        audience_segment_3,
        clicked
    FROM campaign_impressions
    LIMIT 5000000
    """

    with engine.connect() as conn:
        df = pd.read_sql(text(query), conn)

    if df.empty:
        raise ValueError("No training data found. Run etl_pipeline.py first.")
    
    logger.info(f"Loaded {len(df):,} rows for training")

    df = df.dropna()

    for col in FEATURES:
        df[col] = pd.to_numeric(df[col], errors='coerce').fillna(0).astype(int)

    X = df[FEATURES]
    y = df['clicked']

    X_train, X_test, y_train, y_test = train_test_split(
        X, y,
        test_size=0.2,
        random_state=42
    )

    logger.info("Training Gradient Boosting model...")
    model = GradientBoostingClassifier(
        n_estimators=100,
        max_depth=4,
        learning_rate=0.1,
        random_state=42
    )
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)
    accuracy = accuracy_score(y_test, y_pred)
    logger.info(f"Model accuracy: {accuracy:.4f}")
    logger.info(f"\n{classification_report(y_test, y_pred)}")

    joblib.dump(model, MODEL_PATH)
    logger.info("Model saved to disk")

    return len(df), accuracy

@app.on_event("startup")
async def startup_event():
    logger.info("ML Scoring Service starting...")
    loaded = load_model()
    if not loaded:
        logger.warning("No saved model found. Call POST /retrain to train a new model.")

@app.get("/health")
def health_check():
    return {
        "status": "healthy",
        "model_loaded": model is not None,
        "service": "Netflix Ads CTR Prediction"
    }

@app.post("/retrain", response_model=RetrainResponse)
def retrain_model():
    logger.info("Retraining model...")
    try:
        rows, accuracy = train_model()
        return RetrainResponse(
            status="success",
            rows_trained_on=rows,
            accuracy=round(accuracy, 4),
            message=f"Model retrained on {rows:,} rows with {accuracy:.2%} accuracy"
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.error(f"Retraining failed: {e}")
        raise HTTPException(status_code=500, detail=f"Retraining failed: {str(e)}")
    
@app.post("/predict", response_model=PredictResponse)
def predict(request: PredictRequest):
    if model is None:
        raise HTTPException(
            status_code=503,
            detail="Model not loaded. Call POST /retrain first."
        )
    
    input_data = pd.DataFrame([{
        'banner_position':    request.banner_position,
        'device_type':        request.device_type,
        'connection_type':    request.connection_type,
        'audience_segment_1': request.audience_segment_1,
        'audience_segment_2': request.audience_segment_2,
        'audience_segment_3': request.audience_segment_3,
    }])

    ctr_probability = float(model.predict_proba(input_data)[0][1])
    bid_signal = ctr_to_bid_signal(ctr_probability)
    confidence = ctr_to_confidence(ctr_probability)

    return PredictResponse(
        show_id=request.show_id,
        bid_signal=bid_signal,
        ctr_probability=round(ctr_probability, 4),
        confidence=confidence
    )

@app.get("/model/info")
def model_info():
    if model is None:
        return {"status": "No model loaded"}
    return {
        "status": "loaded",
        "model_type": type(model).__name__,
        "features": FEATURES,
        "bid_thresholds": {
            "BID_HIGH":   "CTR >= 8%",
            "BID_NORMAL": "CTR 4% - 8%",
            "BID_LOW":    "CTR < 4%"
        }
    }

if __name__ == '__main__':
    uvicorn.run(
        "ml_scoring_service:app",
        host="0.0.0.0",
        port=8000,
        reload=True
    )