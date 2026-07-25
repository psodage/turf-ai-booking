# ==============================================================================
# Stage 1: Build Java Application JAR
# ==============================================================================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy Maven POM and dependency definitions first for layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production JAR
COPY src ./src
RUN mvn package -DskipTests -B

# ==============================================================================
# Stage 2: Production JRE Runtime Image
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine AS runner

# Create non-root user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy built JAR artifact from builder stage
COPY --from=builder /app/target/turf-ai-booking-*.jar /app/app.jar

# Set directory permissions for reports and storage
RUN mkdir -p /app/reports && chown -R appuser:appgroup /app

USER appuser

# Health check endpoint via Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "/app/app.jar"]
