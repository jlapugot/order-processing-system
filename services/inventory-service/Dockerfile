# Multi-stage Dockerfile for Inventory Service

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy all necessary files for dependency resolution
COPY pom.xml .
COPY services/common-lib/pom.xml services/common-lib/pom.xml
COPY services/inventory-service/pom.xml services/inventory-service/pom.xml

# Copy source code (needed for build)
COPY services/common-lib/src services/common-lib/src
COPY services/inventory-service/src services/inventory-service/src

# Build the application (skip tests for faster builds)
RUN mvn clean package -pl services/inventory-service -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add non-root user for security
RUN addgroup -g 1001 appuser && \
    adduser -D -u 1001 -G appuser appuser

# Copy JAR from build stage
COPY --from=build /app/services/inventory-service/target/*.jar app.jar

# Change ownership
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# JVM options for container
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
