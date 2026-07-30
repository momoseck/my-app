# syntax=docker/dockerfile:1

# ----------------------------------------------------------------------
# Stage 1 - Build the Spring Boot fat jar
# ----------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy the POM first so the Maven cache layer is reused when only source changes.
COPY pom.xml .
COPY src ./src

# Build the jar. The BuildKit cache mount keeps the local Maven repository
# between builds so dependencies are not re-downloaded every time.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q clean package -DskipTests

# ----------------------------------------------------------------------
# Stage 2 - Minimal runtime image
# ----------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# curl is used by the container HEALTHCHECK
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Run as an unprivileged user
RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=build /build/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring

EXPOSE 8080

# JVM options can be tuned at runtime, e.g. JAVA_OPTS="-Xmx512m"
ENV JAVA_OPTS=""

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
