# Multi-stage build for HealthFirst Application
FROM maven:3.9.5-openjdk-21 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:21-jre-slim

# Set working directory
WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r healthfirst && useradd -r -g healthfirst healthfirst

# Create directories
RUN mkdir -p /app/logs /app/uploads && chown -R healthfirst:healthfirst /app

# Copy the built JAR from build stage
COPY --from=build /app/target/health-first-server-*.jar app.jar

# Switch to non-root user
USER healthfirst

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"] 