# Image for Micronaut app (fat JAR is built in CI, app.jar is copied here)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY app.jar app.jar
EXPOSE 8000
ENV MICRONAUT_SERVER_HOST=0.0.0.0
ENV MICRONAUT_SERVER_PORT=8000
ENTRYPOINT ["java", "-jar", "app.jar"]
