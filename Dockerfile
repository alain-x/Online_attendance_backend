# Use official OpenJDK image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy project files
COPY . .

# Build the application using Maven
RUN ./mvnw clean package -DskipTests

# Run the jar file
CMD ["java", "-jar", "target/attendance-0.0.1-SNAPSHOT.jar"]