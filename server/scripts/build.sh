#!/bin/bash

# Build all services
cd ../services/account-service && mvn clean package -DskipTests
cd ../card-service && mvn clean package -DskipTests
cd ../common-library && mvn clean package -DskipTests
cd ../customer-support-service && mvn clean package -DskipTests
cd ../loan-service && mvn clean package -DskipTests
cd ../transaction-service && mvn clean package -DskipTests
cd ../user-service && mvn clean package -DskipTests
