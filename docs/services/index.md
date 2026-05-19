# Services Overview

floci-gcp emulates GCP services on a single port (`4578`). All services use real GCP wire protocols — your existing GCP SDK calls and gcloud CLI commands work without modification.

## Service Matrix

| Service | Protocol | Endpoint |
|---|---|---|
| [Cloud Storage (GCS)](gcs.md) | REST XML (objects) + REST JSON (management) | `/{bucket}/{object}`, `/storage/v1/b/{bucket}` |
| [Pub/Sub](pubsub.md) | gRPC | `google.pubsub.v1.Publisher`, `google.pubsub.v1.Subscriber` |
| [Firestore](firestore.md) | gRPC | `google.firestore.v1.Firestore` |
| [Datastore](datastore.md) | gRPC | `google.datastore.v1.Datastore` |
| [Secret Manager](secret-manager.md) | gRPC + REST JSON | `google.cloud.secretmanager.v1.SecretManagerService` |
| [IAM](iam.md) | REST JSON | `/v1/projects/{project}/serviceAccounts`, `/v1/projects/{project}/serviceAccounts/{account}/keys` |

## Single-Port Design

All services — gRPC and REST — are available on port **4578** via ALPN negotiation:

- `http2=true` — enables HTTP/2 support
- `grpc.server.use-separate-server=false` — gRPC and REST share the same port

Clients using plain HTTP/1.1 are served REST endpoints. Clients using HTTP/2 (gRPC) are served gRPC endpoints. No separate ports or proxy configuration is required.

## Common Setup

Before calling any service, set the appropriate emulator environment variable:

```bash
export PUBSUB_EMULATOR_HOST=localhost:4578
export FIRESTORE_EMULATOR_HOST=localhost:4578
export DATASTORE_EMULATOR_HOST=localhost:4578
export STORAGE_EMULATOR_HOST=http://localhost:4578
export SECRET_MANAGER_EMULATOR_HOST=localhost:4578
```

GCP SDKs automatically bypass credential validation when these variables are set.

For gcloud CLI:

```bash
gcloud config set project floci-local
```

## Auth Bypass

floci-gcp does not validate credentials. All requests are accepted unconditionally. This matches the behavior of GCP official emulators when `*_EMULATOR_HOST` is set.

## Multi-Project Isolation

All resources are namespaced by GCP project ID. Resources in `project-a` are invisible to `project-b`. See [Multi-Project Isolation](../configuration/multi-project.md).
