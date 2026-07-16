FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/target/order-service-1.0.0.jar app.jar
RUN mkdir -p /app/logs && chown -R app:app /app
USER app
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
