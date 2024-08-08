#!/bin/bash

# Run tests for all services
cd ../services/account-service && mvn test
cd ../card-service && mvn test
cd ../common-library && mvn test
cd ../customer-support-service && mvn test
cd ../loan-service && mvn test
cd ../transaction-service && mvn test
cd ../user-service && mvn test
