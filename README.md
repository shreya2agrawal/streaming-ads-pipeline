# Streaming Ads Platform

An ad serving and campaign management platform for a streaming video service.
When a viewer reaches an ad break, a player asks the platform for ads. The platform
finds the ads allowed to serve there, ranks them, records the decision, counts the
ad once it actually appears, and charges the advertiser only when a viewer clicks,
exactly once and never beyond the campaign's budget. Advertisers' servers then report
bookings made after those clicks, and advertisers read their results through a stats
API, authenticated with their own API keys.

Built with Java, Spring Boot, PostgreSQL, React and TypeScript, with Kafka, Redis,
Spark and a Python CTR model in the wider data pipeline.

## How it flows

```text
Player reaches an ad break
  -> POST /api/v1/ad-decisions          (placement, device, show, viewer segments, slots)
  -> candidate retrieval                one bounded query: active campaigns with a bid and
                                        budget left, active and approved creatives
  -> eligibility rules                  status, flight dates, placement, device, creative
                                        type and length, safety approval, targeting
  -> CTR scoring                        behind a scoring interface (a fixed prior today)
  -> ranking                            bid x CTR, one creative per campaign
  -> slot filling + decision record     each slot gets an unguessable slot token
  -> response                           media, impression URL and click URL for each ad

The ad actually appears (image loaded, or video started)
  -> POST /api/v1/impressions/{token}   recorded once; the report survives a closing tab

Viewer clicks the ad
  -> GET /api/v1/clicks/{token}         recorded once, charged at most what is left of
                                        the budget, then 302 to the advertiser

Viewer books on the advertiser's site; the advertiser's server reports it
  -> POST /api/v1/conversions           X-Api-Key identifies the advertiser
                                        counted once per booking, within 30 days of the click
  -> POST /api/v1/conversions/cancellations   reverses the booking, not the click charge

Advertiser reads its results
  -> GET /api/v1/advertiser/stats       X-Api-Key; by campaign, day, creative or placement
```

## Design decisions

For the full architecture, request traces and data model, see
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

- **Hard rules before scoring.** Eligibility is deterministic and cheap; scoring is
  expensive. A model never scores an ad that is not allowed to serve.
- **Scoring is a separate, replaceable step.** It sits behind a `CtrScoringPort` that
  scores all candidates in one batch. Today it returns a fixed prior, so ranking is
  effectively by bid.
- **Every rejection has a reason code**, so "why didn't my ad serve?" is answerable.
- **An impression means the ad really appeared**, reported when the image has loaded or
  the video has started, and counted once per slot.
- **Charged exactly once, never beyond budget.** Clicks are claimed with a unique token
  and `INSERT ... ON CONFLICT DO NOTHING`; charging locks the campaign row; a database
  CHECK (`spent <= budget`) is the final guard.
- **Conversions are measured, not billed.** The advertiser pays for the click; a
  cancelled booking leaves the stats, but does not refund the click.
- **API keys are stored as hashes.** Each key is 32 random bytes, shown once when
  issued; only its SHA-256 hash and a display prefix are kept. Missing, unknown and
  revoked keys all get the same 401.
- **Advertisers only see their own data.** A valid key from another advertiser gets a
  404, exactly as if the click did not exist. Stats take the advertiser from the key,
  never from the request.
- **Stats never fan out.** Each event table is totaled on its own before the totals
  are joined; joining raw tables would multiply the counts. Totals are computed from
  summed counts, never by averaging ratios. Conversion value is kept per currency, and
  return on ad spend only uses value in the billing currency.
- **Check in code, guarantee in the database.** Unique names, one ad per campaign per
  decision, one impression per slot, one conversion per booking, and spend limits are
  all enforced by constraints as well as code.
- **Money is `BigDecimal`**, and **days are platform time zone days**, so a campaign
  ending on a date serves through that whole evening, and a 9 pm click counts on the
  right day.

## Repository layout

```text
ads-platform/        Spring Boot backend: campaigns, creatives, placements, safety
                     review, media plans, inventory, serving (decisions, impressions,
                     clicks, conversions), API keys, advertiser stats
ads-platform-ui/     React + TypeScript operations UI, including the demo player
big-data-python/     ETL, Spark streaming aggregation, Airflow DAG, FastAPI CTR model
scripts/             simulate-advertiser.sh: plays an advertiser's server
```

## Running locally

**Prerequisites:** Java 21, Node.js, Docker.

```bash
# 1. Infrastructure (from ads-platform/). The backend connects to all four at startup.
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
   **safety approved**. For impressions to be recorded, the creative's media must load;
   original demo media is in `ads-platform-ui/public/demo/`.
3. Open **Demo player**, choose the placement and a show the campaign targets, press
   **Play**. The player calls the real decision API, reports the impression when the ad
   appears, and the **Why this ad?** panel explains each winner and each exclusion.
   **Learn more** goes through click tracking.

### Call the APIs directly

```bash
# Request ads (debug=true adds the decision explanation)
curl -s -X POST "localhost:8080/api/v1/ad-decisions?debug=true" \
  -H "Content-Type: application/json" \
  -d '{ "placementId": 1, "deviceType": "TV", "showId": "show-711",
        "audienceSegments": ["HOME_IMPROVEMENT"], "availableSlots": 2 }'

# Report that a served ad appeared, then click it (tokens come from the response)
curl -i -X POST localhost:8080/api/v1/impressions/<token>
curl -i localhost:8080/api/v1/clicks/<token>

# Issue an API key for an advertiser (shown once)
curl -s -X POST localhost:8080/api/v1/advertiser-api-keys \
  -H "Content-Type: application/json" -d '{ "advertiser": "<advertiser name>" }'

# Report and cancel a booking as the advertiser's server
API_KEY=<key> ./scripts/simulate-advertiser.sh book   <click-token> <booking-id> <value>
API_KEY=<key> ./scripts/simulate-advertiser.sh cancel <click-token> <booking-id>

# Read the advertiser's stats
curl -s -H "X-Api-Key: <key>" \
  "localhost:8080/api/v1/advertiser/stats?from=2026-10-01&to=2026-10-31&groupBy=day"
```

## Tests

```bash
cd ads-platform
./mvnw test
```

237 tests. Unit tests cover the entity rules, eligibility rules, ranking, slot filling,
impressions, click charging, conversions, API keys and stats calculations. Integration
tests start the full application against PostgreSQL: the click concurrency tests run
two threads at once to prove a click is charged once and spend never passes the budget,
and the stats integration tests run the real stats SQL for every grouping, so a fan-out
or range bug would fail them. Integration tests need the services from
`docker compose`.

## Status

**Built and tested**

- Campaign, creative, placement, media plan and inventory management
- Creative safety review workflow with review history
- Serving: candidate retrieval, eligibility rules with reason codes, batch scoring
  interface, ranking (bid x CTR, with CTR a fixed prior), slot filling, decision records
- Impressions, counted once, only when the ad really appears
- Click tracking with idempotent, budget-safe charging
- Conversions with a 30-day attribution window and cancellations
- Advertiser API keys: hashed storage, issue, list, revoke, ownership checks
- Advertiser stats by campaign, day, creative and placement
- Demo player with decision explanations; a script that plays an advertiser's server

**Roadmap (being built next)**

Domain events from the serving services; a daily stats rollup reconciled with billing;
a stats dashboard; CTR from the platform's own impressions and clicks; bid multipliers
with a maximum CPC; viewable impressions; fixes for the inventory allocation race and
for reviews of creatives edited mid-review; media plans gating serving.

**Designed, not built**

- Serving events to Kafka through a transactional outbox, feeding Spark
- A Redis serving index and spend counters, once load testing shows they are needed
- Request signing for API keys, so a recorded request cannot be replayed

**Known limitations**

- The offline pipeline trains on a public dataset (Avazu), whose features do not match
  serving requests, so it is not connected to serving yet
- Ownership is checked by advertiser name; there is no separate advertisers table
- The operations endpoints, including issuing API keys, have no admin authentication
- Video completion (quartiles) is not tracked, so a full watch and a brief one count the same