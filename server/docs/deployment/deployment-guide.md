# Deployment Guide

This document provides instructions for deploying Loretta Bank on a Kubernetes cluster.

## Prerequisites

- Docker installed
- Kubernetes cluster setup
- `kubectl` configured
- Access to a Docker registry

## Steps

1. **Build Docker Images:**
   Run the `scripts/build.sh` script to build the Docker images for all services.

2. **Push Images to Registry:**
   Tag and push the images to your Docker registry.

3. **Apply Kubernetes Configurations:**
   Use the `scripts/deploy.sh` script to deploy the services to Kubernetes.

4. **Verify Deployment:**
   Use `kubectl get pods` to ensure all services are running.
