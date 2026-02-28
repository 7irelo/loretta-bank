# Loretta Bank Full Kubernetes Stack

This directory deploys the full app into Kubernetes:

- Frontend (Next.js)
- API gateway + all backend microservices
- PostgreSQL, Kafka, and Redis (single-node, in-cluster)

## Scope and Cost Profile

This setup is optimized for low-cost environments and developer/test use:

- Single replica for all services.
- In-cluster infra (Postgres/Kafka/Redis).
- Ephemeral infra storage (`emptyDir`) to avoid EBS costs.

If a pod is rescheduled, Postgres/Kafka data is lost. Use managed services or PVCs for production.

## Minikube (Local)

Fast path on Windows/PowerShell:

```powershell
.\scripts\deploy-minikube.ps1
```

This script:

- Starts Minikube (if needed)
- Enables ingress addon
- Builds all images locally
- Loads images into Minikube
- Applies `k8s/all`

Then run:

```bash
minikube tunnel -p minikube
```

Add a hosts entry using your Minikube IP:

```txt
<minikube-ip> loretta-bank.local
```

Open `http://loretta-bank.local`.

## Remote Cluster Images

Set your registry prefix once:

```bash
export REGISTRY=ghcr.io/your-org
export TAG=latest
```

This stack defaults to ARM-based EKS nodes (`t4g.*`), so publish ARM-compatible images.

Build and push backend images:

```bash
docker buildx build --platform linux/arm64 -f server/discovery-service/Dockerfile -t $REGISTRY/loretta-discovery-service:$TAG --push server
docker buildx build --platform linux/arm64 -f server/api-gateway/Dockerfile -t $REGISTRY/loretta-api-gateway:$TAG --push server
docker buildx build --platform linux/arm64 -f server/auth-service/Dockerfile -t $REGISTRY/loretta-auth-service:$TAG --push server
docker buildx build --platform linux/arm64 -f server/customer-service/Dockerfile -t $REGISTRY/loretta-customer-service:$TAG --push server
docker buildx build --platform linux/arm64 -f server/account-service/Dockerfile -t $REGISTRY/loretta-account-service:$TAG --push server
docker buildx build --platform linux/arm64 -f server/transaction-service/Dockerfile -t $REGISTRY/loretta-transaction-service:$TAG --push server
docker buildx build --platform linux/arm64 -f server/notification-service/Dockerfile -t $REGISTRY/loretta-notification-service:$TAG --push server
docker buildx build --platform linux/arm64 -f server/audit-service/Dockerfile -t $REGISTRY/loretta-audit-service:$TAG --push server
docker buildx build --platform linux/arm64 -f server/reporting-service/Dockerfile -t $REGISTRY/loretta-reporting-service:$TAG --push server
```

Build and push frontend image:

```bash
docker buildx build --platform linux/arm64 -f client/Dockerfile -t $REGISTRY/loretta-bank-client:$TAG --build-arg NEXT_PUBLIC_API_URL="" --push client
```

`NEXT_PUBLIC_API_URL=""` keeps frontend API calls same-origin (`/api/...`), matching the ingress routing.

## 1. Configure ingress hostname

Edit `k8s/all/ingress.yaml` and change:

- `host: loretta-bank.local` to your domain.

The ingress expects:

- `/` -> `loretta-frontend`
- `/api` -> `api-gateway`

## 2. Update deployment image names

If using a remote registry, update the image names/tags in:

- `k8s/all/frontend.yaml`
- `k8s/all/*-service.yaml`

## 3. Deploy

```bash
kubectl apply -k k8s/all
```

If your cluster does not already have an ingress controller, install one first (for example, `ingress-nginx`).

## 4. Verify

```bash
kubectl -n loretta-bank get pods
kubectl -n loretta-bank get svc
kubectl -n loretta-bank get ingress
```
