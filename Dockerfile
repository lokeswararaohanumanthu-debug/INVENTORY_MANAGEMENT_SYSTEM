# =========================================================================
# Multi-Stage Dockerfile for MobileZone Pro (INVENTRA)
# Compatible with Render, Railway, Fly.io, Koyeb, AWS, and GCP
# =========================================================================

# Stage 1: Build the Spring Boot JAR
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Lightweight Runtime Environment
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/inventory-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /app/data
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
