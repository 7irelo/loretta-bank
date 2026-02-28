# Frontend Kubernetes Deployment

This bundle deploys the Next.js frontend into your Kubernetes cluster and exposes it through an Ingress.

## 1. Build and push the frontend image

`NEXT_PUBLIC_*` values are baked into the frontend at build time.

```bash
docker build \
  -f client/Dockerfile \
  -t ghcr.io/your-org/loretta-bank-client:latest \
  --build-arg NEXT_PUBLIC_API_URL="" \
  client

docker push ghcr.io/your-org/loretta-bank-client:latest
```

Using `NEXT_PUBLIC_API_URL=""` makes browser calls use same-origin paths like `/api/v1/...`, which is recommended when your Ingress also routes `/api` to the gateway.

## 2. Configure manifests

- Update `k8s/frontend/kustomization.yaml` image `newName/newTag`.
- Update `k8s/frontend/ingress.yaml`:
  - `host` to your real domain.
  - `service.name` under `/api` if your gateway service name differs from `api-gateway`.

## 3. Deploy

```bash
kubectl apply -k k8s/frontend
```

## 4. Verify

```bash
kubectl get pods -l app=loretta-frontend
kubectl get svc loretta-frontend
kubectl get ingress loretta-frontend
```
