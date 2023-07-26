#!/bin/bash

# Deploy all services to Kubernetes
kubectl apply -f ../deployment/k8s/account-service/
kubectl apply -f ../deployment/k8s/card-service/
kubectl apply -f ../deployment/k8s/common-library/
kubectl apply -f ../deployment/k8s/customer-support-service/
kubectl apply -f ../deployment/k8s/loan-service/
kubectl apply -f ../deployment/k8s/transaction-service/
kubectl apply -f ../deployment/k8s/user-service/
