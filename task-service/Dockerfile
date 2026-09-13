# syntax=docker/dockerfile:1

# ---------- Stage 1: Build ----------
FROM maven:3.9.8-eclipse-temurin-21-alpine AS build
WORKDIR /build

# Copy pom first to leverage Docker layer caching for dependencies
COPY pom.xml ./

# Pre-fetch dependencies with retry logic to avoid transient Maven Central issues
RUN for i in 1 2 3; do mvn -B dependency:go-offline && break || { echo "Maven dependency fetch failed, retrying..."; sleep 15; }; done

# Copy source and build the application
COPY src/ src/
RUN for i in 1 2 3; do mvn -B clean package -DskipTests && break || { echo "Maven package failed, retrying..."; sleep 15; }; done

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Run as a non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
