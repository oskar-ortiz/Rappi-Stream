FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /workspace
ARG MODULE=notification-router
COPY pom.xml .
COPY order-api/pom.xml order-api/pom.xml
COPY order-processor/pom.xml order-processor/pom.xml
COPY driver-simulator/pom.xml driver-simulator/pom.xml
COPY notification-router/pom.xml notification-router/pom.xml
COPY sms-worker/pom.xml sms-worker/pom.xml
COPY push-notification-worker/pom.xml push-notification-worker/pom.xml
COPY dashboard-aggregator/pom.xml dashboard-aggregator/pom.xml
COPY dashboard-backend/pom.xml dashboard-backend/pom.xml
RUN mvn dependency:go-offline -B
COPY notification-router/src notification-router/src
RUN mvn -B -pl ${MODULE} -am clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S appgroup && adduser -S -G appgroup appuser
WORKDIR /app
COPY --from=build /workspace/notification-router/target/notification-router-0.1.0-SNAPSHOT.jar /app/app.jar
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
USER appuser
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
