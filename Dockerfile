# --- сборка ---
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
RUN mkdir /app/media
ENV MEDIA_ROOT=/app/media
EXPOSE 8080
ENTRYPOINT ["java", "-jar",  "/app/app.jar"]
