# One Dockerfile for every Spring Boot service:
#   docker build --build-arg SERVICE=auth-service -t shopsecure/auth-service .
FROM maven:3.9-eclipse-temurin-21 AS build
ARG SERVICE
WORKDIR /workspace
COPY pom.xml .
COPY config-server/pom.xml config-server/
COPY discovery-server/pom.xml discovery-server/
COPY api-gateway/pom.xml api-gateway/
COPY auth-service/pom.xml auth-service/
COPY product-service/pom.xml product-service/
COPY order-service/pom.xml order-service/
COPY notification-service/pom.xml notification-service/
# Download dependencies in a separate layer so code changes rebuild fast
RUN mvn -q -B -pl ${SERVICE} -am dependency:go-offline -DskipTests || true
COPY . .
RUN mvn -q -B -pl ${SERVICE} -am package -DskipTests && cp ${SERVICE}/target/${SERVICE}-*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine
# Security: never run the app as root inside the container
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
