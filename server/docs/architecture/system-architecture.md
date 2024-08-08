# System Architecture

Loretta Bank is a microservices-based banking system deployed on Kubernetes. The system is composed of the following services:

- Account Service
- Card Service
- Common Library
- Customer Support Service
- Loan Service
- Transaction Service
- User Service

### Kubernetes Deployment

Each service is deployed in its own Kubernetes pod, with configurations managed via ConfigMaps and Secrets. The services communicate with each other using REST APIs.
