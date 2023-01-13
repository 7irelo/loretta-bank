# Use the official Maven image to build the project
FROM maven:3.8.4-openjdk-22 AS build
WORKDIR /app
COPY loan-service/pom.xml .
COPY loan-service/src ./src
RUN mvn clean package -DskipTests

# Use the official OpenJDK image to run the application
FROM openjdk:22-jdk
WORKDIR /app
COPY --from=build /app/target/loanservice.jar ./loanservice.jar
ENTRYPOINT ["java", "-jar", "loanservice.jar"]
