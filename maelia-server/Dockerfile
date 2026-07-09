# maelia-server/Dockerfile — Spring Boot (Java 21), build multi-stage
# Le même artefact sert l'API (SPRING_PROFILES_ACTIVE=api) et le worker (=worker).

# --- Étape 1 : build ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache des dépendances Maven avant de copier les sources
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B -DskipTests package

# --- Étape 2 : runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S maelia && adduser -S maelia -G maelia
USER maelia

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
