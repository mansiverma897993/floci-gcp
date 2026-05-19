# floci-gcp-compatibility-tests

Compatibility test suite for [Floci GCP](https://github.com/hectorvent/floci-gcp) — a local GCP emulator.

Verifies that standard GCP tooling (SDKs) works correctly against the emulator without modification. Tests run against a live Floci GCP instance and use real GCP SDK clients — no mocks.

## Quick Start

```bash
# Install just (task runner)
# macOS: brew install just
# Linux: cargo install just

# Copy and configure environment
cp env.example .env

# Install dependencies
just setup

# Run all tests
just test-all

# Run Java SDK tests
just test-java
```

## Test Runners

| Module                            | Language  | Test Framework | Command           |
| --------------------------------- | --------- | -------------- | ----------------- |
| [`sdk-test-java`](sdk-test-java/) | Java 21   | JUnit 5        | `just test-java`  |

## Test Coverage

The Java test suite covers the following GCP services:

| Test Class          | GCP Service    | Operations                                              |
| ------------------- | -------------- | ------------------------------------------------------- |
| `PubSubTest`        | Pub/Sub        | Create topic, create subscription, publish, pull, delete |
| `GcsTest`           | Cloud Storage  | Create bucket, upload, download, list, delete           |
| `FirestoreTest`     | Firestore      | Set, get, update, query with filter, delete document    |
| `DatastoreTest`     | Datastore      | Put, get, update, query with filter, delete entity      |
| `SecretManagerTest` | Secret Manager | Create secret, add version, access, list versions, delete |

## Prerequisites

- **Floci GCP running** on `http://localhost:4578` (or set `FLOCI_GCP_ENDPOINT`)
- **Java 21+** and **Maven**
- **just** — task runner for orchestration

## Setup

```bash
# Setup all dependencies
just setup

# Resolve Java dependencies manually
just setup-java
```

## Running Tests

### All suites

```bash
just test-all
```

### Java SDK tests only

```bash
just test-java
```

## Configuration

All modules read from environment variables (see `env.example`):

```bash
FLOCI_GCP_ENDPOINT=http://localhost:4578
FLOCI_GCP_PROJECT=test-project
PUBSUB_EMULATOR_HOST=localhost:4578
FIRESTORE_EMULATOR_HOST=localhost:4578
DATASTORE_EMULATOR_HOST=localhost:4578
STORAGE_EMULATOR_HOST=http://localhost:4578
```

The GCP SDKs auto-detect the following emulator env vars:

| Variable                | Service        | Format                 |
| ----------------------- | -------------- | ---------------------- |
| `PUBSUB_EMULATOR_HOST`  | Pub/Sub        | `host:port`            |
| `FIRESTORE_EMULATOR_HOST` | Firestore    | `host:port`            |
| `DATASTORE_EMULATOR_HOST` | Datastore    | `host:port`            |
| `STORAGE_EMULATOR_HOST` | Cloud Storage  | `http://host:port`     |

Secret Manager does not have a GCP-standard emulator env var — the test suite connects via a plaintext gRPC channel configured directly.

## Running with Docker

The Java module includes a `Dockerfile` for isolated execution:

```bash
docker build -t floci-gcp-sdk-java sdk-test-java/
docker run --rm --network host floci-gcp-sdk-java
```

On macOS/Windows, use `host.docker.internal` instead of `localhost`:

```bash
docker run --rm \
  -e FLOCI_GCP_ENDPOINT=http://host.docker.internal:4578 \
  -e PUBSUB_EMULATOR_HOST=host.docker.internal:4578 \
  -e FIRESTORE_EMULATOR_HOST=host.docker.internal:4578 \
  -e DATASTORE_EMULATOR_HOST=host.docker.internal:4578 \
  -e STORAGE_EMULATOR_HOST=http://host.docker.internal:4578 \
  floci-gcp-sdk-java
```

Test results (JUnit XML) are written to `/results/` inside the container. Mount a volume to retrieve them:

```bash
docker run --rm --network host -v $(pwd)/results:/results floci-gcp-sdk-java
```

## Exit Codes

All test runners exit `0` on full pass and non-zero if any test fails — suitable for CI pipelines.

## Note on Test Status

These tests define the expected behavior of the Floci GCP emulator. Tests are expected to fail until the corresponding service is implemented in the emulator. Failing tests indicate which services still need implementation.
