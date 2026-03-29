# Atlas Monitor

A full-stack application for monitoring MongoDB Atlas cluster IOPS metrics via the [Atlas Admin API v2](https://www.mongodb.com/docs/api/doc/atlas-admin-api-v2/).

- **Backend** — Spring Boot REST API
- **Frontend** — Angular dashboard with Chart.js line charts

---

## Features

- **Primary IOPS dashboard** — single merged chart for the primary node across the full queried window, with automatic failover detection
- **Failover annotation** — when a primary role change occurred within the window, a dashed amber "Failover" line is drawn on the chart at the exact timestamp
- **All-replicas view** — a secondary grid shows one chart per replica (primary + secondaries) for side-by-side comparison
- **Automatic primary detection** — identifies which node held the PRIMARY role per time slot via `OPCOUNTER_INSERT`, correctly handling failovers within the queried window
- **Peak & Max detection** — returns the highest average and absolute-max IOPS value with its timestamp for each metric
- **Metric toggles** — show/hide Read, Write, Total series independently per chart (defaults: Read + Write on, Total off)
- **Configurable granularity** — `PT1M`, `PT5M`, `PT1H`, `P1D`
- **Locale toggle** — date/time formatting switchable between `en-US` and `pt-BR` (default: `pt-BR`)

---

## Requirements

| Component | Minimum version |
|-----------|----------------|
| Java | 17 |
| Node.js | 20 |
| A MongoDB Atlas project with an API key | — |

---

## Configuration

The backend reads Atlas credentials from environment variables:

| Variable | Description |
|---|---|
| `ATLAS_PUBLIC_KEY` | Atlas API public key |
| `ATLAS_PRIVATE_KEY` | Atlas API private key |
| `ATLAS_GROUP_ID` | Atlas project (group) ID |

To generate an API key: **Atlas → Access Manager → API Keys** → grant at least the **Project Read Only** role.

---

## Running

### Backend

```bash
export ATLAS_PUBLIC_KEY=your-public-key
export ATLAS_PRIVATE_KEY=your-private-key
export ATLAS_GROUP_ID=your-project-id

./gradlew bootRun
```

Starts on **port 8080**.

### Frontend

```bash
cd frontend
npm install
npm start
```

Opens at **http://localhost:4200**. API calls are proxied to `localhost:8080` via `proxy.conf.json`.

---

## API

### List processes

Returns all hosts in the configured Atlas project.

```
GET /api/v1/processes
```

### List disks for a process

```
GET /api/v1/processes/{processId}/disks
```

### Query IOPS — all instances

Returns time-series IOPS with peak detection for every node in the replica set.

```
GET /api/v1/iops?granularity=PT1H&start=2024-01-01T00:00:00Z&end=2024-01-02T00:00:00Z
```

| Parameter | Required | Default | Description |
|---|---|---|---|
| `granularity` | no | `PT1H` | ISO 8601 duration: `PT1M`, `PT5M`, `PT1H`, `P1D` |
| `start` | yes | — | Start of time window (ISO 8601) |
| `end` | yes | — | End of time window (ISO 8601) |

### Query IOPS — primary only

Returns a **single** `IopsQueryResponse` representing the primary across the entire window.
If a failover occurred, data points from all primary windows are merged into one continuous time series and `roleChanges` lists the timestamps when the primary changed.

```
GET /api/v1/iops/primary?granularity=PT1H&start=2024-01-01T00:00:00Z&end=2024-01-02T00:00:00Z
```

Same parameters as above.

### Query IOPS peaks — primary only

Returns only the peak value and timestamp per metric for the primary, with no time-series data.

```
GET /api/v1/iops/primary/peak?granularity=PT1H&start=2024-01-01T00:00:00Z&end=2024-01-02T00:00:00Z
```

Same parameters as above.

### Response shape (`IopsQueryResponse`)

```jsonc
{
  "processId": "abc123",
  "hostname": "cluster0-shard-00-00.example.mongodb.net:27017",
  "currentRole": "REPLICA_PRIMARY",
  "partitionName": "data",
  "granularity": "PT1H",
  "start": "2024-01-01T00:00:00Z",
  "end": "2024-01-02T00:00:00Z",
  "roleChanges": ["2024-01-01T14:00:00Z"],   // empty when no failover
  "read":     { "dataPoints": [...], "peak": { "timestamp": "...", "value": 123.4 } },
  "write":    { "dataPoints": [...], "peak": { "timestamp": "...", "value": 45.6 } },
  "total":    { "dataPoints": [...], "peak": { "timestamp": "...", "value": 168.9 } },
  "maxRead":  { "dataPoints": [...], "peak": { "timestamp": "...", "value": 200.0 } },
  "maxWrite": { "dataPoints": [...], "peak": { "timestamp": "...", "value": 80.0 } },
  "maxTotal": { "dataPoints": [...], "peak": { "timestamp": "...", "value": 280.0 } }
}
```

---

## Project structure

```
atlas-monitor/
├── src/main/java/com/atlasmonitor/
│   ├── api/
│   │   ├── IopsController.java               # REST endpoints
│   │   └── dto/
│   │       ├── IopsQueryResponse.java         # Full time-series response
│   │       └── IopsPeakResponse.java          # Peak-only response
│   ├── client/
│   │   ├── AtlasApiClient.java               # Atlas Admin API v2 calls
│   │   └── resource/                         # Atlas API response DTOs
│   ├── config/
│   │   ├── AtlasApiProperties.java           # @ConfigurationProperties
│   │   └── RestClientConfig.java             # RestClient with Digest Auth
│   ├── model/
│   │   └── PrimaryWindow.java                # Time window for a primary tenure
│   └── service/
│       ├── IopsService.java                  # IOPS query + merge logic
│       └── PrimaryReplicaResolutionService.java  # Identifies primary per time slot
└── frontend/src/app/
    ├── models/
    │   └── iops.model.ts                     # TypeScript interfaces matching backend DTOs
    ├── services/
    │   └── iops.service.ts                   # HttpClient calls to /api/v1/iops
    └── features/dashboard/
        ├── dashboard.component.ts            # Dashboard logic
        ├── dashboard.component.html          # Template
        └── dashboard.component.scss          # Dark-mode styles
```
