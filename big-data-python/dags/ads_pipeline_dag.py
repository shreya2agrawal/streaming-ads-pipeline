from datetime import datetime, timedelta
from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.operators.http import SimpleHttpOperator
import subprocess
import logging
import os

logger = logging.getLogger(__name__)

# DEFAULT ARGUMENTS

default_args = {
    'owner': 'netflix_ads',
    'depends_on_past': False,
    'start_date': datetime(2026, 1, 1),
    'email_on_failure': False,
    'email_on_retry': False,
    'retries': 1,
    'retry_delay': timedelta(minutes=5),
}

# DAG DEFINITION

dag = DAG(
    'netflix_ads_pipeline',
    default_args = default_args,
    description='Daily ETL and ML retraining pipeline for ads streaming platform',
    schedule='@daily',
    catchup=False,
    tags=['netflix-ads','etl','ml','production']
)

# TASK 1: ETL PIPELINE
# Loading Avazu data

def run_etl_pipeline():
    logger.info("Starting ETL pipeline...")
    etl_script_path = os.path.join(os.path.dirname(__file__),'..','etl_pipeline.py')
    result = subprocess.run(
        ['python',etl_script_path],
        capture_output=True,
        text=True,
        timeout=3600
    )
    if result.returncode != 0:
        logger.error(f"ETL failed:\n{result.stderr}")
        raise Exception(f"ETL pipeline failed: {result.stderr}")
    logger.info(f"ETL complete:\n{result.stdout}")
    return "ETL pipeline completed successfully"

etl_task = PythonOperator(
    task_id = 'run_etl_pipeline',
    python_callable = run_etl_pipeline,
    dag = dag,
)

# TASK 2: ML RETRAINING

retrain_task = SimpleHttpOperator(
    task_id = 'retrain_ml_model',
    http_conn_id = 'ml_scoring_service',
    endpoint = '/retrain',
    method = 'POST',
    headers = {"Content-Type": "application/json"},
    response_check = lambda response: response.status_code == 200,
    log_response = True,
    dag = dag,
)

etl_task >> retrain_task