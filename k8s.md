# Deploying spring-playground to Kubernetes (kind / Docker Desktop)

Guide for putting the services in this monorepo into Kubernetes using **kustomize**.

Current stack at a glance:

| Component | Image source | Notes |
|---|---|---|
| `spring-playground` (8080) | **No Dockerfile yet** — must add one (mirror mock-server's) | OTel/actuator/Redis-starter; hardcodes OTLP to `localhost:4318` |
| `mock-server` (8081) | Has `mock-server/Dockerfile` | No actuator, no OTel |
| `redis` (6379) | `redis:latest` | Only in compose; no code uses it, but the starter's health indicator will probe it |
| `lgtm` (Grafana+OTel stack) | `grafana/otel-lgtm:latest` | Optional in-cluster; or keep it running in Docker and just point OTLP at the host |

---

## 1. Pick your cluster

**Option A — use the cluster you already have (recommended, zero install):**

```bash
kubectl config use-context docker-desktop
```

Docker Desktop's built-in Kubernetes is kind under the hood. Bonus: it **shares the
image store with your Docker daemon**, so locally built images are visible to pods
directly — no push, no `kind load`. Just use `imagePullPolicy: IfNotPresent`.

**Option B — dedicated kind cluster (needs the `kind` CLI, which is not installed):**

```bash
kind create cluster --name spring-playground
# kind nodes have their own containerd → you MUST load images:
kind load docker-image spring-playground:0.0.1 mock-server:0.0.1 --name spring-playground
```

Go with Option A for local dev.

---

## 2. Build the images

`spring-playground` has no Dockerfile. Create one exactly like `mock-server/Dockerfile`
(same two-stage pattern), run from repo root so the build context includes both modules:

```dockerfile
FROM gradle:9.5.1-jdk25 AS build
WORKDIR /workspace
COPY . .
RUN gradle --no-daemon :spring-playground:bootJar -x test

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/spring-playground/build/libs/spring-playground-*.jar app.jar
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

Then:

```bash
docker build -t spring-playground:0.0.1 -f spring-playground/Dockerfile .
docker build -t mock-server:0.0.1 -f mock-server/Dockerfile .
```

(JRE must be `25-jre` — both modules target JDK 25.)

---

## 3. Kustomize layout

```
k8s/
├── base/
│   ├── kustomization.yaml
│   ├── namespace.yaml
│   ├── spring-playground/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   └── configmap.yaml        # OTLP endpoint overrides
│   ├── mock-server/
│   │   ├── deployment.yaml
│   │   └── service.yaml
│   ├── redis/
│   │   ├── deployment.yaml
│   │   └── service.yaml
│   └── lgtm/
│       └── deployment.yaml + service.yaml
└── overlays/
    └── dev/
        └── kustomization.yaml    # patches: replicas, images:, ingress
```

`base/kustomization.yaml`:

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - namespace.yaml
  - spring-playground
  - mock-server
  - redis
  - lgtm            # only if you move the observability stack in-cluster
images:
  - name: spring-playground
    newTag: 0.0.1
  - name: mock-server
    newTag: 0.0.1
commonLabels:
  app.kubernetes.io/managed-by: kustomize
```

---

## 4. What each manifest contains

### spring-playground deployment — the two things that matter

**(a) ConfigMap to fix the hardcoded OTLP endpoint.** `application.yaml` points at
`localhost:4318`, which won't exist in the pod. Override via env vars (relaxed binding
beats the yaml):

```yaml
env:
  - name: MANAGEMENT_OTLP_METRICS_EXPORT_URL
    value: http://lgtm:4318/v1/metrics
  - name: MANAGEMENT_OPENTELEMETRY_TRACING_EXPORT_OTLP_ENDPOINT
    value: http://lgtm:4318/v1/traces
  - name: MANAGEMENT_OPENTELEMETRY_LOGGING_EXPORT_OTLP_ENDPOINT
    value: http://lgtm:4318/v1/logs
```

(Or mount a ConfigMap overriding `application.yaml` via
`SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:/config/` — env vars are simpler.)

**(b) Probes + the Redis trap.** The liveness probe should hit actuator:

```yaml
livenessProbe:
  httpGet: { path: /actuator/health, port: 8080 }
readinessProbe:
  httpGet: { path: /actuator/health, port: 8080 }
```

But because `spring-boot-starter-data-redis` is on the classpath, the Redis health
indicator makes `/actuator/health` return `DOWN` when Redis is absent → probes will
crash-loop the pod. So either deploy `redis` too (below, mirrors your compose), or
disable it:

```yaml
env:
  - name: MANAGEMENT_HEALTH_REDIS_ENABLED
    value: "false"
```

### mock-server

Plain Deployment + ClusterIP Service. No actuator, so probe the root path:

```yaml
livenessProbe:
  httpGet: { path: /, port: 8081 }
```

### redis

Minimal deployment matching compose:

```yaml
image: redis:latest
ports: [containerPort: 6379]
# no PVC needed for local dev; add a volume if you want persistence
```

### Services

All ClusterIP, `port: 80 → targetPort: <container port>`:

```yaml
apiVersion: v1
kind: Service
metadata: { name: spring-playground }
spec:
  selector: { app: spring-playground }
  ports: [{ port: 80, targetPort: 8080 }]
```

### lgtm (optional in-cluster)

Same image as compose; one Deployment + Service exposing **3000** (Grafana),
**4317/4318** (OTLP), **3200** (Tempo), **4040** (Pyroscope), **9090** (Prometheus).
Note: the OTLP env vars above (`http://lgtm:4318`) assume it's in-cluster. If you'd
rather keep LGTM running in Docker Desktop, point OTLP at
`http://host.docker.internal:4318` instead — that works from any container.

---

## 5. Overlay (dev)

```yaml
# overlays/dev/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - ../../base
namespace: spring-playground          # everything lands in your namespace
patches:
  - target: { kind: Deployment, name: spring-playground }
    patch: |
      - op: replace
        path: /spec/replicas
        value: 2
```

---

## 6. Apply & reach it

```bash
kubectl kustomize overlays/dev          # render-only sanity check
kubectl apply -k overlays/dev
kubectl get pods -n spring-playground
```

Then reach the services. Quickest local dev path:

```bash
kubectl port-forward -n spring-playground svc/spring-playground 8080:80
kubectl port-forward -n spring-playground svc/mock-server 8081:80
kubectl port-forward -n spring-playground svc/lgtm 3000:3000   # Grafana
```

If you want a real ingress later:
`kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml`,
then add an `Ingress` resource in the dev overlay (kind's ingress-nginx works on
Docker Desktop's cluster too).

---

## 7. Gotchas checklist

1. **No Dockerfile for spring-playground yet** — that's the first action item.
2. **OTLP `localhost:4318`** in `application.yaml` won't resolve in-cluster — must
   override (env vars above).
3. **Redis health indicator** — deploy Redis or disable the health check, or your
   probes fail.
4. **JDK 25** — keep `eclipse-temurin:25-jre` / `gradle:9.5.1-jdk25` to match both
   modules.
5. **Option A vs B image visibility** — Docker Desktop's built-in k8s sees local
   images; a kind CLI cluster does not (needs `kind load docker-image`).
6. `.dockerignore` already excludes `**/build` and `.gradle`, so contexts stay clean —
   both Dockerfiles must be built from repo **root**.

---

## 8. Istio routing (host-based)

Routing layer: one `Gateway` + one `VirtualService` per app, in `k8s/base/istio/`.
Hosts: `mock.local` → mock-server, `playground.local` → spring-playground.
redis and lgtm carry `sidecar.istio.io/inject: "false"` (pure infra, no mesh needed).

### Install (one-time, needs istioctl)

```bash
curl -L https://istio.io/downloadIstio | sh -   # downloads ./istio-<ver>/bin/istioctl
export PATH=$PWD/istio-<ver>/bin:$PATH
istioctl install --set profile=default -y       # istiod + ingress gateway
```

Note: k8s 1.36 is newer than what Istio's version matrix lists — expect a
"Kubernetes version not supported" warning; install still proceeds.

### Enable injection & restart apps

```bash
kubectl label ns spring-playground istio-injection=enabled
kubectl -n spring-playground rollout restart deploy spring-playground mock-server
kubectl -n spring-playground rollout status deploy spring-playground mock-server
# wait for pods to become 2/2 (app + istio-proxy)
```

### Apply routing manifests

```bash
kubectl apply -k k8s/overlays/dev
```

### Reach it from the browser

Docker Desktop's cluster has no LoadBalancer, so the ingress gateway stays
`pending`. Port-forward it and use a Host header / hosts entry:

```bash
kubectl port-forward -n istio-system svc/istio-ingressgateway 8080:80
# add to C:\Windows\System32\drivers\etc\hosts  (or /etc/hosts in WSL):
#   127.0.0.1 mock.local playground.local
```

Then browse:
- http://mock.local:8080/           → mock-server (try /json, /anything)
- http://playground.local:8080/     → spring-playground (try /ping, /best-offer)

### Verify the mesh

```bash
istioctl proxy-status                 # sidecars connected to istiod
kubectl -n spring-playground logs deploy/mock-server -c istio-proxy --tail=20
```

### Next steps / experiments

- Traffic split between the 2 spring-playground replicas: add pod labels
  (`version: v1/v2`), `DestinationRule` with subsets, then a `VirtualService`
  `http.route[].weight`.
- 503s from the ingress gateway usually mean the sidecar never registered —
  check `istioctl proxy-status` and `kubectl logs -n istio-system deploy/istiod`.

---

## 9. Persistent port-forward (no typing every time)

`kubectl port-forward` is a client-side tunnel — it cannot be made "default" at the
cluster level, and NodePort/LoadBalancer don't help here because the Windows host
can't route to the Docker Desktop VM's bridge network. Instead, run the tunnel as a
hidden background task that auto-starts at logon and auto-retries.

Helper: `tools/pf-istio.ps1` (loops: start tunnel → on exit, retry after 5s; logs to
`%TEMP%\pf-istio.log`).

**Recommended — Startup folder (no admin, already installed):**
`tools/start-pf-istio.vbs` (and `start-pf-argocd.vbs` for the ArgoCD UI on 8090)
launch the scripts hidden at logon. They are copied to
`C:\Users\nf\AppData\Roaming\Microsoft\Windows\Start Menu\Programs\Startup\`,
so they run automatically when you log in — no scheduled task needed.

Stop the tunnel: kill the `powershell.exe` process running `pf-istio.ps1`
(Task Manager → search "pf-istio") or remove the `.vbs` from the Startup folder.

**Alternative — scheduled task (requires admin):**

```powershell
schtasks /Create /TN istio-pf /TR 'powershell.exe -NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File "C:\Users\nf\IdeaProjects\spring-playground\tools\pf-istio.ps1"' /SC ONLOGON /F
schtasks /Run /TN istio-pf          # start now, no logon needed
```

Manage:

```powershell
schtasks /Query /TN istio-pf        # status
schtasks /End /TN istio-pf          # stop (restarts at next logon)
schtasks /Delete /TN istio-pf /F    # unregister permanently
```

Manual fallback — add to your PowerShell profile (`notepad $PROFILE`):

```powershell
function pf-istio { kubectl port-forward -n istio-system svc/istio-ingressgateway 8080:80 }
```

Then just type `pf-istio`. Note: don't run a manual tunnel on 8080 while the task
is active (port conflict) — `schtasks /End /TN istio-pf` first.

---

## 10. ArgoCD (GitOps)

ArgoCD continuously syncs the kustomize overlay in this repo into the cluster.
CLI: `argocd` v3.5.1 (scoop). Server installed from the official manifests:
`kubectl apply --server-side -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/v3.5.1/manifests/install.yaml`
(server-side apply is required — the `applicationsets` CRD is so large that
client-side apply's `last-applied` annotation exceeds the 256KB limit).

### Prerequisite: a reachable git remote (required)

ArgoCD's repo-server runs in-cluster and clones from a URL — it cannot read the
local filesystem, and this repo currently has **no remote**. Push it somewhere:

```bash
# create the repo on GitHub first, then:
git remote add origin https://github.com/<you>/spring-playground.git
git push -u origin master
```

Public repo → no credentials needed. Private repo → register credentials:

```bash
argocd repo add https://github.com/<you>/spring-playground.git \
  --username <you> --password <personal-access-token>
```

### Bootstrap the Application

Edit `k8s/argocd/application.yaml` → set `spec.source.repoURL` to the real URL,
then:

```bash
kubectl apply -k k8s/argocd
argocd app get spring-playground      # watch status (Synced / Healthy)
```

The Application watches `k8s/overlays/dev` (the same kustomize overlay you apply
by hand). `automated.prune` + `selfHeal` are on, so manual `kubectl` changes are
reverted to git and deletions are mirrored.

### Access the UI

```bash
# persistent tunnel (same pattern as §9, port 8090 so it doesn't clash with istio on 8080)
powershell -NoProfile -File .\tools\pf-argocd.ps1
# or ad-hoc:
kubectl port-forward -n argocd svc/argocd-server 8090:443
```

- UI: https://localhost:8090  (admin / initial password below)
- CLI login:
  ```bash
  argocd login localhost:8090
  argocd account update-password   # rotate the admin password
  ```
- Initial admin password:
  ```bash
  kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d
  ```

### Troubleshooting

- `argocd app get` shows `OutOfSync` after you change git → wait one refresh or
  hit **Refresh** in the UI; `selfHeal` fixes drift automatically.
- App stuck `Missing`/`InvalidSpec` → repo not registered or repoURL wrong
  (`argocd repo list`).
- ArgoCD version 3.5.1 matches the installed server; keep CLI and server in sync
  (`argocd version`).
