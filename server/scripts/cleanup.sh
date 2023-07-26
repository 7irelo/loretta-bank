#!/bin/bash

# Cleanup Kubernetes resources
kubectl delete -f ../deployment/k8s/account-service/
kubectl delete -f ../deployment/k8s/card-service/
kubectl delete -f ../deployment/k8s/common-library/
kubectl delete -f ../deployment/k8s/customer-support-service/
kubectl delete -f ../deployment/k8s/loan-service/
kubectl delete -f ../deployment/k8s/transaction-service/
kubectl delete -f ../deployment/k8s/user-service/
