# syntax=docker/dockerfile:1

# ---------- Stage 1: Build ----------
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /build

# Copy wrapper + pom first to leverage Docker layer caching for dependencies
COPY mvnw mvnw.cmd ./
COPY .mvn/ .mvn/
COPY pom.xml ./
RUN chmod +x mvnw

# Pre-fetch dependencies (cached unless pom.xml changes)
RUN ./mvnw -B dependency:go-offline

# Copy source and build the application
COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Run as a non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
