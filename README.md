# Atlas Monitor

A Spring Boot application for monitoring MongoDB Atlas cluster metrics via the [Atlas Admin API v2](https://www.mongodb.com/docs/api/doc/atlas-admin-api-v2/).

## Features

- Query disk **IOPS metrics** (read, write, total) for any node in a replica set
- **Automatic primary detection** — identifies which node held the PRIMARY role during a historical time window using `OPCOUNTER_INSERT`, correctly handling failovers
- **Peak detection** — returns the highest IOPS value and its timestamp within the queried interval
- Configurable **granularity** (PT1M, PT5M, PT1H, P1D) and **date range**

## Requirements

- Java 17+
- A MongoDB Atlas project with an API key

## Configuration

The application reads credentials from environment variables:

| Variable | Description |
|---|---|
| `ATLAS_PUBLIC_KEY` | Atlas API public key |
| `ATLAS_PRIVATE_KEY` | Atlas API private key |
| `ATLAS_GROUP_ID` | Atlas project ID |

To generate an API key, go to **Atlas → Access Manager → API Keys** and grant at least the **Project Read Only** role.

## Running

```bash
export ATLAS_PUBLIC_KEY=your-public-key
export ATLAS_PRIVATE_KEY=your-private-key
export ATLAS_GROUP_ID=your-project-id

./gradlew bootRun
```

The application starts on port `8080`.

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

### Query IOPS

Returns IOPS time-series with peak detection for each matching node.

For `nodeType=PRIMARY`, the primary is identified historically per slot via `OPCOUNTER_INSERT` — if a failover occurred within the window, multiple entries are returned, each scoped to the exact tenure of that primary.

```
GET /api/v1/iops?nodeType=PRIMARY&granularity=PT1H&start=2024-01-01T00:00:00Z&end=2024-01-02T00:00:00Z
```

| Parameter | Required | Default | Description |
|---|---|---|---|
| `nodeType` | yes | — | `PRIMARY` or `SECONDARY` |
| `granularity` | no | `PT1H` | ISO 8601 duration: `PT1M`, `PT5M`, `PT1H`, `P1D` |
| `start` | yes | — | Start of time window (ISO 8601) |
| `end` | yes | — | End of time window (ISO 8601) |

### Query IOPS peaks only

Same parameters as above — returns only the peak value and its timestamp per metric, with no time-series data.

```
GET /api/v1/iops/peak?nodeType=PRIMARY&granularity=PT1H&start=2024-01-01T00:00:00Z&end=2024-01-02T00:00:00Z
```

## Project Structure

```
src/main/java/com/atlasmonitor/
├── api/
│   ├── IopsController.java          # REST endpoints
│   └── dto/                         # Response shapes
├── client/
│   ├── AtlasApiClient.java          # Atlas Admin API v2 calls
│   └── dto/                         # Atlas API response DTOs
├── config/
│   ├── AtlasApiProperties.java      # @ConfigurationProperties
│   └── RestClientConfig.java        # RestClient with Digest Auth
├── model/
│   ├── NodeType.java                # PRIMARY / SECONDARY enum
│   └── PrimaryWindow.java           # Time window for a primary tenure
└── service/
    ├── IopsService.java             # IOPS query logic
    └── PrimaryResolutionService.java # Identifies primary per time slot
```
