FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

ARG SERVICE_DIR
ARG ARTIFACT_ID

COPY mvnw mvnw
COPY .mvn .mvn
COPY pom.xml pom.xml
COPY common/pom.xml common/pom.xml
COPY discovery-service/pom.xml discovery-service/pom.xml
COPY gateway-service/pom.xml gateway-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY field-service/pom.xml field-service/pom.xml
COPY booking-service/pom.xml booking-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml

RUN chmod +x mvnw && ./mvnw -q -pl "${SERVICE_DIR}" -am dependency:go-offline

COPY common common
COPY discovery-service discovery-service
COPY gateway-service gateway-service
COPY user-service user-service
COPY field-service field-service
COPY booking-service booking-service
COPY notification-service notification-service
COPY payment-service payment-service

RUN ./mvnw -q -pl "${SERVICE_DIR}" -am -Dmaven.test.skip=true package && \
    cp "${SERVICE_DIR}"/target/*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY --from=builder /workspace/app.jar /app/app.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
