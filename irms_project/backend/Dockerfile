FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
ENV SPRING_PROFILES_ACTIVE=local,ordering-service \
    IRMS_RUNTIME_MODE=ordering-service \
    IRMS_RUNTIME_SERVICE=ordering-service \
    SERVER_PORT=8080 \
    JAVA_OPTS=""
COPY --from=build /workspace/target/*.jar /app/irms.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${SERVER_PORT:-8080} -jar /app/irms.jar"]
