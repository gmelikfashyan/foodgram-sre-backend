# --- сборка ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# --- запуск ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/foodgram.jar app.jar
RUN mkdir -p /app/media
ENV MEDIA_ROOT=/app/media
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
