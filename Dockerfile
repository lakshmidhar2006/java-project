# Use a standard, lightweight Java 17 base image
FROM eclipse-temurin:17-jdk-jammy

# Set a working directory inside the container
WORKDIR /app

# Copy the Maven wrapper files first to leverage Docker's layer caching
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy the rest of the application's source code
COPY src ./src

# Package the application into a JAR file
RUN ./mvnw package -DskipTests

# Expose the port the application runs on
EXPOSE 8080

# The command to run the application
CMD ["java", "-jar", "target/demo-0.0.1-SNAPSHOT.jar"]