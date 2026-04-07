# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
# Copy Maven dependencies first for better caching
COPY pom.xml .
COPY src ./src
# Build the project, skipping tests for faster deployment speeds
RUN mvn clean package -DskipTests

# Stage 2: Create a lightweight runtime container
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Copy the compiled .jar file from the build stage
COPY --from=build /app/target/*.jar app.jar
# Expose the standard Spring Boot port
EXPOSE 8080
# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
