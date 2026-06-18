# Netflix Ads Platform

A full-stack advertising platform simulating Netflix's ad-supported tier. Advertisers buy ad slots inside Netflix shows. The platform manages campaigns, tracks inventory, predicts click-through rates, and processes ad impression events in real time.

Built with Java Spring Boot, React, Apache Kafka, PySpark, and scikit-learn.

---

## Architecture

```
React UI (TypeScript)
        │  REST API
        ▼
Spring Boot Backend (Java 21)
  ├── Campaigns, Inventory, Media Plans, Brand Safety
  ├── Kafka → publishes events on every state change
  ├── PostgreSQL → persistent storage
  ├── Redis → inventory slot caching
  └── OpenAI → AI-driven insights
        │
        ▼
Python Data Engineering Layer
  ├── ETL Pipeline → loads 40M Avazu impression rows into PostgreSQL
  ├── ML Scoring Service (FastAPI) → CTR prediction, BID_HIGH/NORMAL/LOW
  ├── Spark Pipeline → reads Kafka impression-events, 5-min window aggregation
  └── Airflow DAG → daily ETL + ML retraining schedule
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 19, TypeScript, Recharts, React Router |
| Backend | Java 21, Spring Boot 3.5, Lombok |
| Database | PostgreSQL 16, Redis |
| Messaging | Apache Kafka |
| Stream Processing | Apache Spark 3.5 (PySpark Structured Streaming) |
| ML Service | Python, FastAPI, scikit-learn (Gradient Boosting) |
| Orchestration | Apache Airflow |
| Schema Management | Flyway |
| Infrastructure | Docker Compose |

---

## Project Structure

```
Netflix Ads Project/
│
├── ads-platform/                        # Java Spring Boot backend
│   ├── src/main/java/com/adsplatform/
│   │   ├── campaign/                    # Campaign lifecycle management
│   │   ├── inventory/                   # Slot tracking + ML bid signals
│   │   ├── mediaplan/                   # Media plan + flight scheduling
│   │   ├── safety/                      # Brand safety flags
│   │   └── kafka/                       # Event consumers
│   ├── src/main/resources/
│   │   ├── application.yaml             # Spring Boot config + Flyway
│   │   └── db/migration/
│   │       └── V1__init_schema.sql      # Full schema (Flyway managed)
│   └── docker-compose.yml               # PostgreSQL, Redis, Kafka, Zookeeper, Airflow
│
├── ads-platform-ui/                     # React TypeScript frontend
│   └── src/
│       ├── pages/                       # Overview, Campaigns, Inventory, Safety, Insights, MediaPlan
│       ├── components/                  # Sidebar, MetricCard, StatusBadge
│       └── api/                         # Axios client + typed API calls
│
└── big-data-python/                     # Python data engineering layer
    ├── requirements.txt                 # All Python dependencies
    ├── etl_pipeline.py                  # Batch ETL: Avazu CSV → PostgreSQL (40M rows)
    ├── ml_scoring_service.py            # FastAPI CTR prediction microservice
    ├── spark_pipeline.py                # PySpark Structured Streaming: Kafka → PostgreSQL
    └── dags/
        └── ads_pipeline_dag.py          # Airflow DAG: daily ETL + ML retraining
```

---

## Data Flow

```
1. Advertiser creates campaign via React UI
2. Spring Boot saves to PostgreSQL, publishes event to Kafka
3. Ad is served → impression event published to impression-events Kafka topic
4. Spark reads impression-events continuously
5. Spark aggregates into 5-minute windows (impressions, clicks, CTR per show)
6. Spark writes aggregated results to campaign_metrics_aggregated table
7. ML service reads this table and retrains CTR prediction model
8. Spring Boot calls /predict → returns BID_HIGH / BID_NORMAL / BID_LOW
9. React UI shows bid signals on Inventory page
```

---

## Dataset

The ETL pipeline uses the **Avazu CTR Prediction** dataset (Kaggle) — 40 million real ad impression rows with click labels. This dataset is not included in the repository due to its size (5.9 GB).

Download it from: `https://www.kaggle.com/c/avazu-ctr-prediction/data`

Place the extracted file at: `big-data-python/data/train.csv`

---

## Getting Started

### Prerequisites

- Java 21+
- Node.js 18+
- Python 3.11+
- Docker + Docker Compose

### 1. Start all infrastructure

```bash
cd ads-platform
docker-compose up -d
```

Starts PostgreSQL (5433), Redis (6379), Kafka (9092), Zookeeper, and Airflow (8090).

### 2. Run the Spring Boot backend

```bash
cd ads-platform
export OPENAI_API_KEY=your_key_here
./mvnw spring-boot:run
```

Flyway automatically runs `V1__init_schema.sql` on first startup and creates all tables.

Backend runs on `http://localhost:8080`

### 3. Run the React frontend

```bash
cd ads-platform-ui
npm install
npm start
```

UI runs on `http://localhost:3000`

### 4. Set up the Python environment

```bash
cd big-data-python
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

### 5. Run the ETL pipeline

Download the Avazu dataset and place it at `big-data-python/data/train.csv`, then:

```bash
python etl_pipeline.py
```

Loads 40 million rows into PostgreSQL in ~90 minutes using 100K-row chunks.

### 6. Run the ML scoring service

```bash
python ml_scoring_service.py
```

Service runs on `http://localhost:8000`

Train the model after ETL completes:

```bash
curl -X POST http://localhost:8000/retrain
```

### 7. Create the Kafka impression-events topic

```bash
docker exec ads-platform-kafka-1 kafka-topics \
  --create --topic impression-events \
  --bootstrap-server localhost:9092 \
  --partitions 1 --replication-factor 1
```

### 8. Run the Spark pipeline

```bash
python spark_pipeline.py
```

Spark connects to Kafka, reads impression events, aggregates them in 5-minute windows, and writes results to PostgreSQL continuously.

---

## API Reference

### Spring Boot Backend (`localhost:8080`)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/campaigns` | List all campaigns |
| POST | `/api/campaigns` | Create campaign |
| PATCH | `/api/campaigns/:id/status` | Update campaign status |
| GET | `/api/inventory` | List inventory with fill rates |
| GET | `/api/inventory/stats` | Aggregate inventory stats |
| GET | `/api/safety/flags` | Brand safety flags |
| POST | `/api/safety/check` | Check content safety |
| GET | `/api/media-plans` | List media plans |
| POST | `/api/media-plans` | Create media plan |

### ML Scoring Service (`localhost:8000`)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/health` | Service health + model status |
| POST | `/predict` | Predict BID_HIGH/NORMAL/LOW for an ad slot |
| POST | `/retrain` | Retrain model on latest PostgreSQL data |
| GET | `/model/info` | Model type, features, thresholds |

---

## ML Model

- **Algorithm**: Gradient Boosting Classifier (scikit-learn)
- **Training data**: 5 million rows from Avazu CTR dataset
- **Accuracy**: 82.76%
- **Features**: banner position, device type, connection type, audience segments
- **Output**:
  - `BID_HIGH` — predicted CTR ≥ 8%
  - `BID_NORMAL` — predicted CTR 4–8%
  - `BID_LOW` — predicted CTR < 4%

---

## Kafka Topics

| Topic | Producer | Consumer | Purpose |
|---|---|---|---|
| `campaign-events` | Spring Boot | CampaignEventConsumer | Campaign lifecycle events |
| `mediaplan-events` | Spring Boot | MediaPlanEventConsumer | Media plan state changes |
| `safety-events` | Spring Boot | SafetyEventConsumer | Brand safety flags |
| `impression-events` | Spring Boot | Spark pipeline | Real-time ad impression events |

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `OPENAI_API_KEY` | — | Required for AI insights feature |
| `DATABASE_URL` | `postgresql://ads_user:ads_pass@localhost:5433/adsplatform` | Python services DB connection |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker address |
| `KAFKA_TOPIC` | `impression-events` | Spark pipeline topic |

---

## Author

Shreya Agrawal — [agrawal.shrey@northeastern.edu](mailto:agrawal.shrey@northeastern.edu)

GitHub: [shreya2agrawal/streaming-ads-pipeline](https://github.com/shreya2agrawal/streaming-ads-pipeline)