import pandas as pd
import sqlalchemy 
from sqlalchemy import create_engine, text
import logging
import os
from dotenv import load_dotenv
from datetime import datetime
import sys

# LOGGING SETUP
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# CONFIGURATION
load_dotenv()
DB_URL = os.getenv(
    'DATABASE_URL',
    'postgresql://ads_user:ads_pass@localhost:5433/adsplatform'
)
AVAZU_FILE_PATH = os.getenv(
    'AVAZU_FILE_PATH',
    'data/train.csv'
)
CHUNK_SIZE = 100_000

# AVAZU COLUMN MAPPING 
# Avazu name : campaign_impressions table field
COLUMN_MAPPING = {
    'id': 'impression_id',
    'click': 'clicked',
    'hour': 'event_hour',
    'banner_pos': 'banner_position',
    'site_id': 'show_id',
    'site_category': 'show_category',
    'app_id': 'app_id',
    'device_type': 'device_type',
    'device_conn_type': 'connection_type',
    'C1': 'audience_segment_1',
    'C14': 'audience_segment_2',
    'C15': 'audience_segment_3',
    'C16': 'audience_segment_4',
    'C17': 'audience_segment_5',
    'C18': 'audience_segment_6',
    'C19': 'audience_segment_7',
    'C20': 'audience_segment_8',
    'C21': 'audience_segment_9',
}

COLUMNS_TO_KEEP = list(COLUMN_MAPPING.keys())

# DATABASE CONNECTION
def create_db_engine():
    try:
        engine = create_engine(DB_URL, pool_size=5, max_overflow=10, pool_timeout=30, pool_recycle=3600, connect_args={})
        logger.info("Database connection successful")
        return engine
    except Exception as e:
        logger.error(f"Database connection failed: {e}")
        sys.exit(1)

# TABLE CREATION
def create_impressions_table(engine):
    """ Staging Raw Avazu Data"""
    create_table_sql = """
    CREATE TABLE IF NOT EXISTS campaign_impressions (
        impression_id       VARCHAR(50)      PRIMARY KEY,
        clicked             SMALLINT         NOT NULL DEFAULT 0,
        -- 1 = clicked, 0 = not clicked
        event_hour          VARCHAR(10)      NOT NULL,
        event_timestamp     TIMESTAMP,
        banner_position     SMALLINT,
        show_id             VARCHAR(50),
        show_category       VARCHAR(50),
        app_id              VARCHAR(50),
        device_type         SMALLINT,
        connection_type     SMALLINT,
        audience_segment_1  INTEGER,
        audience_segment_2  INTEGER,
        audience_segment_3  INTEGER,
        audience_segment_4  INTEGER,
        audience_segment_5  INTEGER,
        audience_segment_6  INTEGER,
        audience_segment_7  INTEGER,
        audience_segment_8  INTEGER,
        audience_segment_9  INTEGER,
        loaded_at           TIMESTAMP        NOT NULL DEFAULT NOW()
    );

    CREATE INDEX IF NOT EXISTS idx_impressions_show_id 
        ON campaign_impressions(show_id);

    CREATE INDEX IF NOT EXISTS idx_impressions_clicked
        ON campaign_impressions(clicked);
    
    CREATE INDEX IF NOT EXISTS idx_impressions_timestamp
        ON campaign_impressions(event_timestamp);
    """
    with engine.connect() as conn:
        conn.execute(text(create_table_sql))
        conn.commit()

    logger.info("campaign_impressions table created")

# TIMESTAMP CONVERSION

def parse_avazu_hour(hour_str):
    """ 
    Avazu format: '14102100' 
    Year=2014, Month=10, Day=21, Hour=00
    Output: datetime(2014, 10, 21, 0, 0, 0)
    """
    try:
        return datetime.strptime(str(hour_str),'%y%m%d%H')
    except:
        return None

# DATA CLEANING

def clean_chunk(df):
    # Removing extra columns
    available_cols = [c for c in COLUMNS_TO_KEEP if c in df.columns]
    df = df[available_cols].copy()
    # Renaming columns
    mapping = {k: v for k, v in COLUMN_MAPPING.items() 
               if k in df.columns}
    df = df.rename(columns=mapping)
    # Removing nulls
    df = df.dropna(subset=['show_id','clicked'])
    # Parsing time stamps
    if 'event_hour' in df.columns:
        df['event_timestamp'] = df['event_hour'].apply(parse_avazu_hour)
        df.dropna(subset=['event_timestamp'])
    # Fixing data types for clicked
    if 'clicked' in df.columns:
        df['clicked'] = pd.to_numeric(
            df['clicked'], errors='coerce'
        ).fillna(0).astype(int)
    # Device_type and banner_position should be integers
    for col in ['device_type','banner_position','connection_type']:
        if col in df.columns:
            df[col] = pd.to_numeric(
                df[col], errors='coerce'
            ).fillna(0).astype(int)
    return df

# AGGREGATION

def aggregate_to_metrics(engine):
    logger.info("Starting aggregation to campaign_metrics_aggregated")
    aggregation_sql = """
    INSERT INTO campaign_metrics_aggregated
    (show_id, window_start, window_end, impressions, clicks, ctr)

    SELECT 
        show_id,
        date_trunc('hour', event_timestamp) +
            INTERVAL '5 min' *
            FLOOR(
                EXTRACT(MINUTE FROM event_timestamp) / 5
            ) AS window_start,
        date_trunc('hour', event_timestamp) +
            INTERVAL '5 min' *
            FLOOR(
                EXTRACT (MINUTE FROM event_timestamp)/5
            ) + INTERVAL '5 min' AS window_end,
        COUNT(*) AS impressions,
        SUM(clicked) AS clicks,
        -- CTR = clicks / impressions
        ROUND(
            SUM(clicked)::DECIMAL/
            NULLIF(COUNT(*),0), 4
        ) AS ctr
    FROM campaign_impressions
    GROUP BY show_id, 
    date_trunc(\'hour\', event_timestamp) +
        INTERVAL \'5 min\' *
        FLOOR(EXTRACT(MINUTE FROM event_timestamp) / 5)
    """

    with engine.connect() as conn:
        conn.execute(text(aggregation_sql))
        conn.commit()
    
    logger.info("Aggregation complete — campaign_metrics_aggregated updated")

# MAIN ETL FUNCTION

def run_etl():
    logger.info("=" * 50)
    logger.info("ETL Pipeline starting")
    logger.info(f"Source file: {AVAZU_FILE_PATH}")
    logger.info(f"Chunk size: {CHUNK_SIZE:,} rows")
    logger.info("=" * 50)

    engine = create_db_engine()
    create_impressions_table(engine)

    if not os.path.exists(AVAZU_FILE_PATH):
        logger.error(
            f"Failed to find Avazu file: {AVAZU_FILE_PATH}\n"
            f"Download from Kaggle and store it here:\n"
            f"big-data-python/data/train.csv"
        )
        sys.exit(1)
    
    total_rows_loaded = 0
    chunk_number = 0

    logger.info("Reading Avazu CSV in chunks...")

    for chunk in pd.read_csv(AVAZU_FILE_PATH, chunksize=CHUNK_SIZE, dtype=str):
        chunk_number += 1
        logger.info(f"Processing batch {chunk_number}...")
        cleaned = clean_chunk(chunk)
        if cleaned.empty:
            logger.warning(
                f"Batch {chunk_number} empty after cleaning — skipping"
            )
            continue

        # Writing to PostgreSQL
        cleaned.to_sql(
            'campaign_impressions',
            engine,
            if_exists='append',
            index=False,
            method='multi',
            chunksize=1000
        )

        total_rows_loaded += len(cleaned)
        logger.info(
            f"Batch {chunk_number} done — "
            f"{len(cleaned):,} rows loaded. "
            f"Total so far: {total_rows_loaded:,}"
        )

    logger.info("=" * 50)
    logger.info(f"CSV loading complete — {total_rows_loaded:,} total rows loaded")

    # Aggregation
    aggregate_to_metrics(engine)

    logger.info("ETL Pipeline complete!")
    logger.info("=" * 50)

# ENTRY POINT 
if __name__ == '__main__':
    run_etl()