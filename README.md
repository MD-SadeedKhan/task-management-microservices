# Task Management Microservices

A small, deliberately minimal microservices project used to learn the path:

```
Spring Boot → Docker → Docker Compose → Kubernetes → GitHub Actions → GHCR → (later) GitOps/Argo CD
```

## 1. Architecture

```
      ┌────────────────┐
      │    frontend      │  nginx: static SPA + reverse proxy
      │   (port 80/3000)  │  serves the UI, forwards /users, /tasks,
      └────────┬────────┘  /actuator/health to the gateway
               │
      ┌────────▼────────┐
      │      gateway      │  Spring Cloud Gateway
      │   (port 8080)      │  routes /users/** and /tasks/**
      └────────┬────────┘
    ┌───────────┴───────────┐
    │                        │
┌───▼──────────┐    ┌────────▼─────┐
│ user-service   │    │ task-service   │
│ (port 8081)     │    │ (port 8082)     │
│ Controller→      │    │ Controller→      │
│ Service→          │    │ Service→          │
│ Repository→       │    │ Repository→       │
│ Entity              │    │ Entity              │
└───┬──────────┘    └────────┬─────┘
    │                        │
┌───▼──────────┐    ┌────────▼─────┐
│ user-postgres  │    │ task-postgres  │
│ db: userdb      │    │ db: taskdb      │
└──────────────┘    └──────────────┘
```

Design principles:

- **No service discovery** (no Eureka/Consul). The gateway is statically configured with the target hosts/ports via environment variables that default to the Docker Compose / Kubernetes service names.
- **No shared database.** Each service owns its schema; `task-service` stores `userId` as a plain `Long` — there is no cross-service JPA relationship and no network call between the two services.
- **No security layer** (no Spring Security/OAuth/JWT) — this is intentionally out of scope for this stage of the learning path.
- **Plain constructor injection** everywhere — no Lombok, no MapStruct, no field injection.
- **The frontend never talks to user-service or task-service directly** — it only ever calls the gateway, exactly like any other API client would.

## 2. Project structure

```
task-management-microservices/
├── .github/workflows/
│   ├── ci.yml           # build + test on PRs and pushes to main
│   └── docker.yml       # build + push images to GHCR on push to main
├── gateway/              # Spring Cloud Gateway (WebFlux), port 8080
├── user-service/         # REST API for users, port 8081
├── task-service/         # REST API for tasks, port 8082
├── frontend/             # Static HTML/CSS/JS UI, served by nginx
├── k8s/                  # Kubernetes manifests
├── docker-compose.yml
├── .gitignore
└── README.md
```

Each service is an independent Maven project (own `pom.xml`, own Maven Wrapper) that can be built and run in isolation.

## 3. Tech stack

| Concern            | Choice                              |
|---------------------|--------------------------------------|
| Language             | Java 21 (LTS)                        |
| Framework            | Spring Boot 3.5.6                    |
| Gateway              | Spring Cloud Gateway 2025.0.0 (WebFlux, `spring-cloud-starter-gateway-server-webflux`) |
| Build tool           | Maven (via Maven Wrapper)            |
| Database             | PostgreSQL 16 (`postgres:16-alpine`) |
| Containerization     | Docker / Docker Compose              |
| Orchestration        | Kubernetes                           |
| CI/CD                | GitHub Actions                       |
| Container registry   | GitHub Container Registry (GHCR)     |
| Frontend              | Plain HTML/CSS/vanilla JS, served by nginx (no build step, no framework) |

## 4. Running locally with Maven

Each service can be run standalone. Postgres must be reachable at the configured `DB_HOST`/`DB_PORT` for `user-service` and `task-service` (or override with env vars to point at a local Postgres instance).

```bash
# Terminal 1 - user-service (expects Postgres reachable, defaults shown)
cd user-service
DB_HOST=localhost DB_PORT=5433 DB_NAME=userdb DB_USERNAME=postgres DB_PASSWORD=postgres ./mvnw spring-boot:run

# Terminal 2 - task-service
cd task-service
DB_HOST=localhost DB_PORT=5434 DB_NAME=taskdb DB_USERNAME=postgres DB_PASSWORD=postgres ./mvnw spring-boot:run

# Terminal 3 - gateway
cd gateway
USER_SERVICE_HOST=localhost USER_SERVICE_PORT=8081 TASK_SERVICE_HOST=localhost TASK_SERVICE_PORT=8082 ./mvnw spring-boot:run
```

Run the test suite for a single service:

```bash
cd user-service
./mvnw clean test
```

### Frontend (no build step required)

The frontend is plain HTML/CSS/JS with no framework and no npm build. To run it against a gateway started as above, serve `frontend/src/` with any static file server and either:

- run it through a reverse proxy that forwards `/users`, `/tasks`, and `/actuator/health` to the gateway (this is exactly what the provided nginx config does — see below), or
- for quick local hacking only, edit the `fetch(...)` calls in `app.js` to point at `http://localhost:8080/...` directly.

The Docker/Kubernetes paths below already handle the proxying for you, so this is only relevant if you want to iterate on the UI without containers.

## 5. Running with Docker Compose

This is the easiest way to run the whole stack, including both databases:

```bash
docker compose up --build
```

This starts, in order (via `depends_on` + healthchecks):

1. `user-postgres` (`postgres:16-alpine`, named volume `user-postgres-data`)
2. `task-postgres` (`postgres:16-alpine`, named volume `task-postgres-data`)
3. `user-service` (port `8081`)
4. `task-service` (port `8082`)
5. `gateway` (port `8080`)
6. `frontend` (nginx, port `3000` on the host → container port `80`)

All inter-service communication uses Docker Compose **service names** (`user-postgres`, `task-postgres`, `user-service`, `task-service`, `gateway`) — never `localhost`.

Open the UI at **http://localhost:3000**. The nginx container serves the static SPA and reverse-proxies `/users`, `/tasks`, and `/actuator/health` to `http://gateway:8080` — the browser never talks to `user-service` or `task-service` directly.

Tear down (and optionally remove volumes):

```bash
docker compose down          # stop and remove containers
docker compose down -v       # also remove the named Postgres volumes
```

## 6. Testing the APIs

The quickest way is to open the UI at **http://localhost:3000** (Docker Compose) and use the forms — it lists, creates, and deletes both users and tasks, and shows a live gateway status indicator.

To exercise the API directly, use the gateway on port `8080`:

```bash
# Create a user
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Ada Lovelace","email":"ada@example.com"}'

# List users
curl http://localhost:8080/users

# Get a single user
curl http://localhost:8080/users/1

# Delete a user
curl -X DELETE http://localhost:8080/users/1

# Create a task
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Write report","description":"Q3 report","status":"TODO","userId":1}'

# List tasks
curl http://localhost:8080/tasks

# Get a single task
curl http://localhost:8080/tasks/1

# Delete a task
curl -X DELETE http://localhost:8080/tasks/1
```

`status` accepts one of: `TODO`, `IN_PROGRESS`, `DONE`.

You can also call each service directly (bypassing the gateway) on `8081`/`8082` while developing.

Health checks (Spring Boot Actuator):

```bash
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness
```

## 7. Building Docker images manually

Each service has its own multi-stage `Dockerfile` (Maven build stage → slim JRE runtime stage, non-root user):

```bash
docker build -t task-management-gateway:local ./gateway
docker build -t task-management-user-service:local ./user-service
docker build -t task-management-task-service:local ./task-service
docker build -t task-management-frontend:local ./frontend
```

## 8. Running on Kubernetes

Manifests live in `k8s/`. Apply them in order (or `kubectl apply -f k8s/` — Kubernetes will retry until dependent objects exist):

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/configmaps.yaml

kubectl apply -f k8s/user-postgres-pvc.yaml
kubectl apply -f k8s/user-postgres-deployment.yaml
kubectl apply -f k8s/user-postgres-service.yaml

kubectl apply -f k8s/task-postgres-pvc.yaml
kubectl apply -f k8s/task-postgres-deployment.yaml
kubectl apply -f k8s/task-postgres-service.yaml

kubectl apply -f k8s/user-deployment.yaml
kubectl apply -f k8s/user-service.yaml

kubectl apply -f k8s/task-deployment.yaml
kubectl apply -f k8s/task-service.yaml

kubectl apply -f k8s/gateway-deployment.yaml
kubectl apply -f k8s/gateway-service.yaml

kubectl apply -f k8s/frontend-deployment.yaml
kubectl apply -f k8s/frontend-service.yaml
```

**Before applying**, replace the placeholder image references in `user-deployment.yaml`, `task-deployment.yaml`, `gateway-deployment.yaml`, and `frontend-deployment.yaml` (`ghcr.io/<github-owner>/task-management-*:<tag>`) with a real image built by `docker.yml`, e.g.:

```
ghcr.io/your-github-username/task-management-gateway:1a2b3c4d5e6f...
```

Check status:

```bash
kubectl get pods -n task-management
kubectl get svc -n task-management
```

Access the UI and gateway locally:

```bash
# On a local cluster (kind/minikube) the LoadBalancer EXTERNAL-IP usually
# stays <pending>. Port-forward instead:
kubectl port-forward -n task-management svc/frontend 3000:80
kubectl port-forward -n task-management svc/gateway 8080:8080

# minikube alternative:
minikube tunnel
```

Both `frontend` and `gateway` are exposed as `LoadBalancer` services here for learning convenience (so you can hit the raw API and the UI independently). In a real deployment you'd typically expose only one public entry point — e.g. an Ingress in front of `frontend`, with `gateway` kept as `ClusterIP`.

What each manifest does:

| File                              | Purpose                                             |
|-------------------------------------|------------------------------------------------------|
| `namespace.yaml`                    | Isolates all objects in the `task-management` namespace |
| `configmaps.yaml`                   | Non-sensitive config: DB host/port/name, downstream service host/port |
| `secrets.yaml`                      | DB credentials (Postgres bootstrap + app `DB_USERNAME`/`DB_PASSWORD`) |
| `user-postgres-pvc.yaml`            | Persistent storage for the user database              |
| `user-postgres-deployment.yaml`     | `postgres:16-alpine` for `userdb`, with liveness/readiness `pg_isready` probes |
| `user-postgres-service.yaml`        | Internal ClusterIP service for `user-postgres`        |
| `task-postgres-pvc.yaml`            | Persistent storage for the task database               |
| `task-postgres-deployment.yaml`     | `postgres:16-alpine` for `taskdb`                      |
| `task-postgres-service.yaml`        | Internal ClusterIP service for `task-postgres`         |
| `user-deployment.yaml`              | `user-service` pods with Actuator liveness/readiness probes |
| `user-service.yaml`                 | Internal ClusterIP service for `user-service`          |
| `task-deployment.yaml`              | `task-service` pods with Actuator liveness/readiness probes |
| `task-service.yaml`                 | Internal ClusterIP service for `task-service`          |
| `gateway-deployment.yaml`           | `gateway` pods with Actuator liveness/readiness probes |
| `gateway-service.yaml`              | Externally reachable `LoadBalancer` service            |
| `frontend-deployment.yaml`          | `frontend` (nginx) pods serving the UI and proxying to the gateway |
| `frontend-service.yaml`             | Externally reachable `LoadBalancer` service for the UI |

## 9. How GitHub Actions works

### `ci.yml` — Continuous Integration
Triggers on every pull request and on pushes to `main`. For each of the three services (matrix build):
1. Checks out the code
2. Sets up Java 21 (Temurin)
3. Caches the local Maven repository
4. Runs `./mvnw -B clean verify` (compiles, runs unit tests, fails the build on any test/compile failure)

### `docker.yml` — Build & Push to GHCR
Triggers on push to `main` only. For each of the four services (gateway, user-service, task-service, frontend):
1. Checks out the code
2. Sets up Docker Buildx
3. Logs in to GHCR using the automatically-provided `GITHUB_TOKEN` (no long-lived registry credentials/PATs)
4. Builds the image from that service's `Dockerfile`
5. Pushes it to GHCR tagged with the Git commit SHA (`github.sha`)

Resulting image names:

```
ghcr.io/<github-owner>/task-management-gateway:<commit-sha>
ghcr.io/<github-owner>/task-management-user-service:<commit-sha>
ghcr.io/<github-owner>/task-management-task-service:<commit-sha>
ghcr.io/<github-owner>/task-management-frontend:<commit-sha>
```

Note: `ci.yml` (build/test) only covers the three Java services — the frontend is a static, build-free app with nothing to compile or unit test, so it's built and pushed by `docker.yml` but not part of the Maven test matrix.

This workflow does **not** deploy to Kubernetes — that step is intentionally left for a human (or later, Argo CD) to do explicitly.

## 10. Why commit-SHA tags instead of `latest`

- `latest` is mutable — the same tag can silently point at a different image tomorrow, which makes rollbacks and debugging unreliable.
- A commit SHA tag is immutable and traceable: given a running image, you can always find the exact source code that produced it (`git show <sha>`).
- It's a prerequisite for GitOps: tools like Argo CD compare the desired manifest (an image tag) against a Git commit, which only works reliably with immutable, unique tags.

## 11. Current CI/CD flow

```
GitHub
   ↓
GitHub Actions (ci.yml)  — build & test on every PR / push to main
   ↓
GitHub Actions (docker.yml) — build image + push to GHCR (push to main only)
   ↓
GHCR (ghcr.io/<owner>/task-management-*:<commit-sha>)
   ↓
Kubernetes — manually applied by a human (kubectl apply -f k8s/), with the
             image tag updated by hand to the new commit SHA
```

## 12. Where GitOps / Argo CD fits in later

The current flow stops at GHCR — a human still has to update the image tag in the Kubernetes manifests and run `kubectl apply`. The next stage in this learning path is to close that loop:

1. Move `k8s/` into its own Git repository (or a dedicated path), treated as the single source of truth for what's running in the cluster.
2. Install Argo CD in the cluster and point an `Application` resource at that repository/path.
3. Have the `docker.yml` workflow (or a follow-up workflow) automatically update the image tag in the manifests repository after a successful push to GHCR (e.g. via a small script or a tool like `kustomize edit set image`), and commit that change.
4. Argo CD detects the Git change and automatically syncs the cluster to match — deployments become "git push and it ships" instead of manual `kubectl apply`.

No Argo CD or GitOps tooling is installed yet; this project deliberately stops at "image is in GHCR, manifests exist, a human applies them" so each layer of the stack can be understood on its own before automating the last mile.

## 13. A note on validation

This project was generated and reviewed for internal consistency (matching ports, service names, environment variable names, image names, and Kubernetes selectors/labels across all layers). All Kubernetes manifests and `docker-compose.yml` were validated for YAML syntax, and all `pom.xml` files were validated for XML well-formedness. Full `./mvnw clean test`/`package`, `docker compose build`, and `kubectl` schema validation should be run in an environment with unrestricted access to Maven Central, Docker Hub, and a running cluster before relying on this in production.
