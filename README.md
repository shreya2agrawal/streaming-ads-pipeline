# Streaming Ads Platform

An ad serving and campaign management platform for a streaming video service.
When a viewer reaches an ad break, a player asks the platform for ads. The platform
finds the ads allowed to serve there, ranks them by expected value, records the
decision, and charges the advertiser only when a viewer clicks, exactly once and
never beyond the campaign's budget.

Built with Java, Spring Boot, PostgreSQL, React and TypeScript, with Kafka, Redis,
Spark and a Python CTR model in the wider data pipeline.

## How a request flows

```text
Player reaches an ad break
  -> POST /api/v1/ad-decisions          (placement, device, show, viewer segments, slots)
  -> candidate retrieval                one bounded query: active campaigns with a bid and
                                        budget left, active and approved creatives
  -> eligibility rules                  status, flight dates, placement, device, creative
                                        type and length, safety approval, targeting
  -> CTR scoring                        predicted click-through rate per candidate
  -> ranking                            bid x pCTR, one creative per campaign
  -> slot filling + decision record     each slot gets an unguessable click token
  -> response                           media and click URL for each ad

Viewer clicks the ad
  -> GET /api/v1/clicks/{token}
  -> click recorded once                a repeated click is never charged again
  -> campaign charged                   at most what is left of the budget
  -> 302 redirect to the advertiser
```

## Design decisions

- **Hard rules before scoring.** Eligibility is deterministic and cheap; scoring is
  expensive. A model never scores an ad that is not allowed to serve.
- **The model predicts, the platform decides.** Scoring sits behind a `CtrScoringPort`
  and only returns probabilities. Bids, ranking and budgets are business logic in Java.
- **One class per eligibility rule.** Rules implement `EligibilityRule` and run in
  order. Adding a rule needs no change to the engine.
- **Every rejection has a reason code.** `IneligibilityReason` codes make
  "why didn't my ad serve?" answerable, and countable.
- **Charged exactly once.** Clicks are claimed with a unique token and
  `INSERT ... ON CONFLICT DO NOTHING`, so a retried or double click is charged once.
- **Never beyond budget.** Charging locks the campaign row, so concurrent clicks cannot
  both read the old spend. A database CHECK (`spent <= budget`) is the final guard.
- **Check in code, guarantee in the database.** Unique placement names, one ad per
  campaign per decision, and spend limits are all enforced by constraints as well as code.
- **Entities protect their own rules.** For example, `Placement` has no setters; it can
  only be created or changed through methods that validate everything first.
- **Money is `BigDecimal`.** Floating point cannot represent cents exactly.
- **Flight dates use a platform time zone**, not UTC, so a campaign ending on a date
  serves through that whole evening.

## Repository layout

```text
ads-platform/        Spring Boot backend: campaigns, creatives, placements, safety
                     review, media plans, inventory, serving (decisions and clicks)
ads-platform-ui/     React + TypeScript operations UI, including the demo player
big-data-python/     ETL, Spark streaming aggregation, Airflow DAG, FastAPI CTR model
```

## Running locally

**Prerequisites:** Java 21, Node.js, Docker.

```bash
# 1. Infrastructure (from ads-platform/)
cd ads-platform
docker compose up -d postgres redis zookeeper kafka

# 2. Backend (applies database migrations on startup)
./mvnw spring-boot:run

# 3. UI (in a second terminal)
cd ads-platform-ui
npm install
npm start
```

The backend runs on `http://localhost:8080`, the UI on `http://localhost:3000`.
Database migrations are in `ads-platform/src/main/resources/db/migration` and run
automatically through Flyway.

### Try the demo player

1. In the UI, make sure a placement is **active** (Placements page).
2. Give a campaign a bid and activate it, and give it a creative that is **active** and
   **safety approved** (Creatives page, then the Brand safety review).
3. Open **Demo player**, choose the placement and a show the campaign targets, press
   **Play**. At the ad break, the player calls the real decision API, and the
   **Why this ad?** panel shows the score of each winner and why other campaigns
   were excluded. **Learn more** goes through click tracking.

### Call the API directly

```bash
# Request ads (debug=true adds the decision explanation)
curl -s -X POST "localhost:8080/api/v1/ad-decisions?debug=true" \
  -H "Content-Type: application/json" \
  -d '{
    "placementId": 1,
    "deviceType": "TV",
    "showId": "show-711",
    "audienceSegments": ["HOME_IMPROVEMENT"],
    "availableSlots": 2
  }'

# Click a served ad (use a clickUrl from the response)
curl -i localhost:8080/api/v1/clicks/<token>
```

## Tests

```bash
cd ads-platform
./mvnw test
```

Unit tests cover the entity rules, eligibility rules, ranking, slot filling and click
charging. Integration tests start the full application, and the click concurrency tests
run two threads against PostgreSQL to verify that a click is charged once and that
concurrent clicks never overspend. The integration tests need the local PostgreSQL from
`docker compose`.

## Status

**Built and tested**

- Campaign, creative, placement, media plan and inventory management
- Creative safety review workflow with review history
- Serving: candidate retrieval, eligibility rules with reason codes, ranking, slot filling
- Decision recording (including no-fills), click tracking with idempotent, budget-safe charging
- Demo player with decision explanations

**Designed, next**

- Impression tracking, then a CTR model trained on the platform's own impressions and clicks
  (the current scorer uses a fixed prior)
- Per-campaign time zones, including viewer-local scheduling for global campaigns
- Campaign targeting as placement and device pairs, so impossible combinations cannot be saved
- Typed decision and click events to Kafka through a transactional outbox
- Redis serving index and spend counters once load testing shows the database is the bottleneck

**Known limitations**

- The offline pipeline trains on a public display-ads dataset (Avazu), whose features do
  not match the serving request, so the model is not yet wired into serving
- A few known pipeline issues (ETL reruns, Spark aggregate upserts) are tracked for cleanup