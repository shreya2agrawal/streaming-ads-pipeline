from pyspark.sql import SparkSession
from pyspark.sql.functions import (
    from_json, col, window, count, sum as spark_sum,
    round as spark_round, current_timestamp, lit
)
from pyspark.sql.types import (
    StructType, StructField, StringType,
    IntegerType, TimestampType, LongType
)
import logging
import os
from dotenv import load_dotenv

load_dotenv()

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# CONFIGURATION

KAFKA_BOOTSTRAP_SERVERS = os.getenv(
    'KAFKA_BOOTSTRAP_SERVERS',
    'localhost:9092'
)

KAFKA_TOPIC = os.getenv(
    'KAFKA_TOPIC',
    'impression-events'
)

DB_URL = os.getenv(
    'DATABASE_URL',
    'postgresql://ads_user:ads_pass@localhost:5433/adsplatform'
)

JDBC_URL = os.getenv(
    'JDBC_URL',
    'jdbc:postgresql://localhost:5433/adsplatform'
)

JDBC_DRIVER = 'org.postgresql.Driver'
JDBC_USER = os.getenv('DB_USER','ads_user')
JDBC_PASSWORD = os.getenv("DB_PASSWORD",'ads_pass')

CHECKPOINT_DIR = os.getenv(
    'CHECKPOINT_DIR',
    '/tmp/spark_checkpoints'
)

WINDOW_DURATION = '5 minutes'

WATERMARK_DURATION = '10 minutes'

# IMPRESSION EVENT SCHEMA

IMPRESSION_SCHEMA = StructType([
    StructField('impression_id', StringType(), True),
    StructField('campaign_id', LongType(), True),
    StructField('show_id', StringType(), True),
    StructField('clicked', IntegerType(), True),
    StructField('banner_position', IntegerType(), True),
    StructField('device_type', IntegerType(), True),
    StructField('event_timestamp', TimestampType(), True),
])

# SPARK SESSION

def create_spark_session():
    logger.info("Creating Spark session...")
    spark = SparkSession.builder \
        .appName("NetflixAdsPlatform-StreamingPipeline") \
        .master("local[*]") \
        .config(
            "spark.jars.packages",
            "org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.1,"
            "org.postgresql:postgresql:42.7.3"
        ) \
        .config("spark.sql.shuffle.partitions", "4") \
        .config(
            "spark.streaming.stopGracefullyOnShutdown",
            "true"
        ) \
        .getOrCreate()
    spark.sparkContext.setLogLevel("WARN")
    logger.info("Spark session created successfully")
    return spark

# KAFKA STREAM READER

def read_kafka_stream(spark):
    logger.info(f"Connecting to Kafka topic: {KAFKA_TOPIC}")
    raw_stream = spark \
        .readStream \
        .format("kafka") \
        .option(
            "kafka.bootstrap.servers",
            KAFKA_BOOTSTRAP_SERVERS
        ) \
        .option("subscribe", KAFKA_TOPIC) \
        .option("startingOffsets", "latest") \
        .option("failOnDataLoss","false") \
        .load()
    
    string_stream = raw_stream.selectExpr(
        "CAST(value AS STRING) as json_value",
        "timestamp as kafka_timestamp"
    )

    parsed_stream = string_stream.select(
        from_json(
            col("json_value"),
            IMPRESSION_SCHEMA
        ).alias("data"),
        col("kafka_timestamp")
    ).select(
        "data.*",
        "kafka_timestamp"
    )
    logger.info("Kafka stream reader configured")
    return parsed_stream

# STREAM PROCESSING

def process_stream(parsed_stream):
    logger.info("Configuring stream processing...")
    aggregated_stream = parsed_stream \
        .withWatermark(
            "event_timestamp",
            WATERMARK_DURATION
        ) \
        .groupBy(
            window(
                col("event_timestamp"), 
                WINDOW_DURATION
            ),
            col("show_id"),
            col("campaign_id")
        ) \
        .agg(
            count("*").alias('impressions'),
            spark_sum("clicked").alias("clicks"),
            spark_round(
                spark_sum("clicked").cast("double") /
                count("*"), 
                4
            ).alias("ctr")
        ) \
        .select(
            col("window.start").alias("window_start"),
            col("window.end").alias("window_end"),
            col("show_id"),
            col("campaign_id"),
            col("impressions"),
            col("clicks"),
            col("ctr")
        )
    logger.info("Stream processing configured")
    return aggregated_stream

# WRITING TO POSTGRESQL

def write_batch_to_postgres(batch_df, batch_id):
    if batch_df.isEmpty():
        logger.info(f"Batch {batch_id}: empty, skipping")
        return
    row_count = batch_df.count()
    logger.info(
        f"Batch {batch_id}: writing {row_count} rows to PostgreSQL"
    )
    batch_df.write \
        .format("jdbc") \
        .option("url", JDBC_URL) \
        .option(
            "dbtable",
            "campaign_metrics_aggregated"
        ) \
        .option("user", JDBC_USER) \
        .option("password", JDBC_PASSWORD) \
        .option("driver", JDBC_DRIVER) \
        .mode("append") \
        .save()
    logger.info(f"Batch {batch_id}: write complete")

# MAIN PIPELINE

def run_pipeline():
    logger.info("=" * 50)
    logger.info("Netflix Ads Spark Streaming Pipeline starting")
    logger.info(f"Kafka: {KAFKA_BOOTSTRAP_SERVERS}")
    logger.info(f"Topic: {KAFKA_TOPIC}")
    logger.info(f"Window: {WINDOW_DURATION}")
    logger.info("=" * 50)
    spark = create_spark_session()
    raw_stream = read_kafka_stream(spark)
    aggregated_stream = process_stream(raw_stream)
    query = aggregated_stream \
        .writeStream \
        .outputMode("update") \
        .foreachBatch(write_batch_to_postgres) \
        .option(
            "checkpointLocation",
            CHECKPOINT_DIR
        ) \
        .trigger(processingTime="1 minute") \
        .start()
    logger.info("Streaming pipeline started successfully")
    logger.info("Pipeline running... Press Ctrl+C to stop")
    query.awaitTermination()

# ENTRY POINT

if __name__ == '__main__':
    run_pipeline()