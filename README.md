# Service Side Events Dashboards

Real-time logistics dashboards built with Spring Boot 4, Spring Data JDBC, Angular 21, and MySQL.

## Tech Stack

- Backend: Java 25, Spring Boot 4, Spring Data JDBC, Flyway, Caffeine cache
- Frontend: Angular 21 (standalone components), TypeScript, SCSS
- Database: MySQL 8
- CI: GitHub Actions (`sse-api` tests + `sse-web` build)

## Why SSE for dashboards

- Lower overhead than polling for near-live dashboard updates.
- Native browser support via `EventSource`, with automatic reconnect semantics.
- Simple one-way stream model fits operational read dashboards.
- Works efficiently with cached read models (`@Cacheable`) that refresh on a fixed cadence.

## Dashboard streams

The backend exposes snapshot and stream endpoints:

- `GET /api/dashboards/overview`
- `GET /api/dashboards/inventory-alerts`
- `GET /api/dashboards/shipment-status`
- `GET /api/dashboards/kpi-trend`
- `GET /api/dashboards/stream/overview`
- `GET /api/dashboards/stream/inventory-alerts`
- `GET /api/dashboards/stream/shipment-status`
- `GET /api/dashboards/stream/kpi-trend`
- `POST /api/ingestion/shipment-events` (idempotent write endpoint)

Versioned aliases are also available for dashboard reads and streams:

- `GET /api/v1/dashboards/overview`
- `GET /api/v1/dashboards/inventory-alerts`
- `GET /api/v1/dashboards/shipment-status`
- `GET /api/v1/dashboards/kpi-trend`
- `GET /api/v1/dashboards/stream/overview`
- `GET /api/v1/dashboards/stream/inventory-alerts`
- `GET /api/v1/dashboards/stream/shipment-status`
- `GET /api/v1/dashboards/stream/kpi-trend`
- `PATCH /api/shipments/{trackingNumber}/status` (optimistic locking writes)
- `PATCH /api/v1/shipments/{trackingNumber}/status` (versioned alias)

Each SSE stream emits `DashboardEventEnvelope` payloads with:

- `stream`: stream name
- `sequence`: monotonic event sequence number
- `payload`: current snapshot for that stream

## UUIDs and idempotency

- Core table primary keys use UUID strings (`VARCHAR(36)`) for safer distributed write patterns.
- `Idempotency-Key` is supported on `POST /api/ingestion/shipment-events`.
- Repeating the same idempotency key returns the same `shipmentEventId` without inserting duplicates.

## Optimistic locking for concurrent updates

- `shipments` includes a `version` column.
- Status updates require `expectedVersion` in the request body.
- Update succeeds only when `expectedVersion` matches current DB version.
- On stale version, the API returns `409 Conflict`.

Example request:

```json
{
  "status": "DELIVERED",
  "expectedVersion": 0,
  "delayed": false
}
```

## Local setup

### 1) Start MySQL

```bash
docker compose up -d
```

### 2) Run API (`sse-api`)

```bash
cd sse-api
./mvnw spring-boot:run
```

On Windows:

```powershell
cd sse-api
.\mvnw.cmd spring-boot:run
```

### 3) Run web (`sse-web`)

```bash
cd sse-web
npm install
npm start
```

Open [http://localhost:4200](http://localhost:4200).

## Configuration

API defaults are in `sse-api/src/main/resources/application.properties` and profile files.

Important environment variables:

- `DB_URL` (default: `jdbc:mysql://localhost:3306/logistics_dashboards?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`)
- `DB_USER` (default: `logistics`)
- `DB_PASSWORD` (default: `logistics`)
- `APP_CORS_ALLOWED_ORIGINS` (default: `http://localhost:4200`)
- `DASHBOARD_STREAM_INTERVAL_MS` (default: `5000`)
- `DASHBOARD_DEMO_STREAM_ENABLED` (default: `true` in `dev`, `false` otherwise)
- `DASHBOARD_DEMO_STREAM_INTERVAL_MS` (default: `4000`)

When demo stream is enabled, the backend continuously mutates shipment, inventory, and KPI data so SSE dashboards visibly update in real time.

## Verification

- API tests: `cd sse-api && ./mvnw test`
- Web build: `cd sse-web && npm run build`
