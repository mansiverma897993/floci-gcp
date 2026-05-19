# Environment Variables Reference

floci-gcp is configured through environment variables. Every option maps to a `FLOCI_GCP_*` variable — no YAML file is needed when running the published Docker image.

---

## Global

| Variable | Default | Description |
|---|---|---|
| `FLOCI_GCP_PORT` | `4578` | Port for all services (gRPC + REST) |
| `FLOCI_GCP_BASE_URL` | `http://localhost:4578` | Base URL embedded in service responses (GCS object URLs, etc.) |
| `FLOCI_GCP_HOSTNAME` | _(none)_ | Overrides only the hostname part of `FLOCI_GCP_BASE_URL`. Set to the Compose service name so other containers can reach floci-gcp by DNS |
| `FLOCI_GCP_DEFAULT_PROJECT_ID` | `floci-local` | Default GCP project ID used when no project is specified in the request |

---

## Storage

| Variable | Default | Description |
|---|---|---|
| `FLOCI_GCP_STORAGE_MODE` | `memory` | Global storage backend: `memory`, `persistent`, `hybrid`, or `wal` |
| `FLOCI_GCP_STORAGE_PERSISTENT_PATH` | `./data` | Container-side directory for persistent and hybrid storage |
| `FLOCI_GCP_STORAGE_WAL_COMPACTION_INTERVAL_MS` | `30000` | How often (ms) the WAL compaction runs. Only applies when `FLOCI_GCP_STORAGE_MODE=wal` |

See [Storage Modes](./storage.md) for a full explanation of each mode.

---

## DNS

floci-gcp's embedded DNS server runs inside the container and resolves GCS virtual-hosted style URLs to floci-gcp's container IP. It only activates when running inside Docker.

| Built-in suffix | Covers |
|---|---|
| `localhost.floci.io` | `localhost.floci.io` and `*.localhost.floci.io` (e.g. `my-bucket.localhost.floci.io`) |

| Variable | Default | Description |
|---|---|---|
| `FLOCI_GCP_DNS_EXTRA_SUFFIXES` | _(none)_ | Comma-separated list of additional hostname suffixes to resolve to floci-gcp's container IP |

---

## Services

### Cloud Storage (GCS)

| Variable | Default | Description |
|---|---|---|
| `FLOCI_GCP_SERVICES_GCS_ENABLED` | `true` | Enable/disable Cloud Storage |

### Pub/Sub

| Variable | Default | Description |
|---|---|---|
| `FLOCI_GCP_SERVICES_PUBSUB_ENABLED` | `true` | Enable/disable Pub/Sub |

### Firestore

| Variable | Default | Description |
|---|---|---|
| `FLOCI_GCP_SERVICES_FIRESTORE_ENABLED` | `true` | Enable/disable Firestore |

### Datastore

| Variable | Default | Description |
|---|---|---|
| `FLOCI_GCP_SERVICES_DATASTORE_ENABLED` | `true` | Enable/disable Datastore |

### Secret Manager

| Variable | Default | Description |
|---|---|---|
| `FLOCI_GCP_SERVICES_SECRETMANAGER_ENABLED` | `true` | Enable/disable Secret Manager |

### IAM

| Variable | Default | Description |
|---|---|---|
| `FLOCI_GCP_SERVICES_IAM_ENABLED` | `true` | Enable/disable IAM |

---

## Docker Daemon

These variables control the Docker daemon used by floci-gcp's embedded DNS and container management features.

| Variable | Default | Description |
|---|---|---|
| `FLOCI_GCP_DOCKER_DOCKER_HOST` | `unix:///var/run/docker.sock` | Docker daemon socket path or TCP address |
| `FLOCI_GCP_DOCKER_DOCKER_CONFIG_PATH` | _(none)_ | Path to a directory containing Docker's `config.json` for registry auth |
| `FLOCI_GCP_DOCKER_LOG_MAX_SIZE` | `10m` | Log rotation max size for spawned containers |
| `FLOCI_GCP_DOCKER_LOG_MAX_FILE` | `3` | Number of rotated log files to keep |
