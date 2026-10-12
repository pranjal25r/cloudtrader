# Start from a small image that already has Java 21 installed
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the jar that "mvn package" built
COPY target/portfolio-service.jar app.jar

EXPOSE 8082

# Command that runs when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]
