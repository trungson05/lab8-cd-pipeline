# Docker Compose builds the application from a clean checkout.
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /src
COPY app/pom.xml .
COPY app/src ./src
RUN mvn -B -ntp clean verify

FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -g 1001 -S appgroup && adduser -S appuser -u 1001 -G appgroup
USER appuser
EXPOSE 3000
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -q --spider "http://127.0.0.1:${PORT:-3000}/health" || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# Jenkins packages and fingerprints this JAR before building the image.
FROM runtime AS ci
COPY app/target/devops-lab8-app.jar /app/app.jar

FROM runtime AS standalone
COPY --from=build /src/target/devops-lab8-app.jar /app/app.jar
