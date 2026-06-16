# Netflix Ads Platform

An AI-integrated advertising platform built with a Java/Spring Boot backend, React frontend, and Python big data components. Manages campaigns, inventory, media plans, and brand safety checks — with real-time event streaming and ML-powered CTR prediction.

---

## Architecture

```
React UI (TypeScript)
      │  REST
      ▼
Spring Boot Backend (Java 21)
  ├── Kafka     → event streaming (campaign, safety, media plan events)
  ├── PostgreSQL → persistent storage (campaigns, inventory, media plans)
  ├── Redis     → vector store for brand safety similarity search
  ├── OpenAI    → AI-driven insights and safety classification
  └── ML Service → CTR prediction (Python FastAPI)
            │
            ▼
  Spark Pipeline (Python)
  ├── Reads from Kafka (campaign-events topic)
  ├── Aggregates metrics in 5-min sliding windows
  └── Writes to PostgreSQL (campaign_metrics_aggregated)
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 19, TypeScript, Recharts, React Router |
| Backend | Java 21, Spring Boot 3.5, Spring AI, Lombok |
| Streaming | Apache Kafka |
| Storage | PostgreSQL 16, Redis (vector store) |
| AI | OpenAI API, Spring AI |
| Big Data | Apache Spark 3.5 (PySpark) |
| ML Service | Python, FastAPI, scikit-learn |

---

## Project Structure

```
Netflix Ads Project/
├── ads-platform/               # Java Spring Boot backend
│   ├── src/main/java/com/adsplatform/
│   │   ├── campaign/           # Campaign CRUD + Kafka consumer
│   │   ├── inventory/          # Inventory management
│   │   ├── mediaplan/          # Media planning
│   │   ├── safety/             # Brand safety with vector search
│   │   ├── kafka/              # Kafka event consumers
│   │   └── config/             # Redis vector store config
│   ├── docker-compose.yml      # Postgres, Redis, Kafka, Zookeeper
│   └── application.yaml.example
│
├── ads-platform-ui/            # React TypeScript frontend
│   └── src/
│       ├── pages/              # Overview, Campaigns, Inventory, Safety, Insights, MediaPlan
│       ├── components/         # Sidebar, MetricCard, StatusBadge
│       └── api/                # Axios client + typed API calls
│
└── big-data-python/            # Python big data components
    ├── spark_pipeline.py       # PySpark streaming pipeline (Kafka → PostgreSQL)
    ├── ml_scoring_service.py   # FastAPI CTR prediction microservice
    └── requirements-ml.txt
```

---

## Getting Started

### Prerequisites
- Java 21+
- Node.js 18+
- Python 3.10+
- Docker + Docker Compose

### 1. Start infrastructure

```bash
cd ads-platform
docker-compose up -d
```

This starts PostgreSQL (port 5433), Redis (6379), Kafka (9092), and Zookeeper.

### 2. Configure the backend

```bash
cp application.yaml.example src/main/resources/application.yaml
# Edit application.yaml and set your DB credentials and OpenAI API key
export OPENAI_API_KEY=your_key_here
```

### 3. Run the Java backend

```bash
cd ads-platform
./mvnw spring-boot:run
# Runs on http://localhost:8080
```

### 4. Run the React frontend

```bash
cd ads-platform-ui
npm install
npm start
# Runs on http://localhost:3000
```

### 5. Run the ML Scoring Service

```bash
cd big-data-python
pip install -r requirements-ml.txt
uvicorn ml_scoring_service:app --host 0.0.0.0 --port 8000 --reload
# Runs on http://localhost:8000
# Docs at http://localhost:8000/docs
```

### 6. Run the Spark Pipeline

```bash
cd big-data-python
spark-submit \
  --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.0,org.postgresql:postgresql:42.7.3 \
  spark_pipeline.py
```

---

## Key Features

- **Campaign Management** — full lifecycle tracking with pacing alerts (AT_RISK, PACING_LOW)
- **Brand Safety** — vector similarity search using Redis to flag unsafe ad placements
- **Inventory Management** — available/reserved/sold tracking across content types
- **Media Planning** — budget allocation and flight scheduling
- **AI Insights** — OpenAI-powered campaign recommendations
- **Real-time Streaming** — Kafka event pipeline connecting all services
- **CTR Prediction** — ML model predicts click-through rate per ad slot (BID_HIGH / BID_NORMAL / BID_LOW)
- **Big Data Aggregation** — Spark processes high-volume impression events in 5-min windows

---

## API Endpoints (Java Backend)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/campaigns` | List all campaigns |
| POST | `/api/campaigns` | Create campaign |
| GET | `/api/inventory` | List inventory |
| GET | `/api/safety/flags` | Brand safety flags |
| POST | `/api/safety/check` | Check content safety |
| GET | `/api/media-plans` | List media plans |

## ML Service Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/health` | Model health + training stats |
| POST | `/predict` | Predict CTR for an ad slot |
| POST | `/retrain` | Retrain on latest PostgreSQL data |

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `OPENAI_API_KEY` | — | Required for AI features |
| `POSTGRES_URL` | `localhost:5433` | PostgreSQL connection |
| `POSTGRES_USER` | `ads_user` | DB username |
| `POSTGRES_PASSWORD` | — | DB password (never commit this) |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers |

---

## Roadmap

- [ ] Spark pipeline integration with live Kafka topics
- [ ] ML model retraining schedule (daily via cron)
- [ ] Audience segmentation with clustering
- [ ] Grafana dashboard for Spark metrics
- [ ] CI/CD pipeline with GitHub Actions

---

## Author

Shreya Agrawal — [agrawal.shrey@northeastern.edu](mailto:agrawal.shrey@northeastern.edu)
