FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl audit-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /workspace/audit-service/target/*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
