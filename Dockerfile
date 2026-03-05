# Stage 1: Build the application
# We use the JDK image because it has Maven-compatible tools
FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /build

# Install Maven manually since there isn't a 25-maven image yet
RUN apt-get update && apt-get install -y maven

# Copy and build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create the final image
FROM eclipse-temurin:25-jre-jammy
# Re-adding the security layer
RUN useradd -m h3user
USER h3user
WORKDIR /app

# Copy the jar from the "build" stage
COPY --from=build /build/target/*.jar app.jar

# JVM flags updated for Java 25 memory management
ENTRYPOINT ["java", "-Xmx300M", "-Xms300M", "-jar", "app.jar"]