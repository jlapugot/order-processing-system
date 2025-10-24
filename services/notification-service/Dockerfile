# Multi-stage Dockerfile for Notification Service

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy all necessary files for dependency resolution
COPY pom.xml .
COPY services/common-lib/pom.xml services/common-lib/pom.xml
COPY services/notification-service/pom.xml services/notification-service/pom.xml

# Copy source code (needed for build)
COPY services/common-lib/src services/common-lib/src
COPY services/notification-service/src services/notification-service/src

# Build the application (skip tests for faster builds)
RUN mvn clean package -pl services/notification-service -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add non-root user for security
RUN addgroup -g 1001 appuser && \
    adduser -D -u 1001 -G appuser appuser

# Copy JAR from build stage
COPY --from=build /app/services/notification-service/target/*.jar app.jar

# Change ownership
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port (notification service doesn't expose HTTP, but keeping for consistency)
EXPOSE 8082

# JVM options for container
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
