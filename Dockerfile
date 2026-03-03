FROM eclipse-temurin:25-jdk-jammy
RUN useradd -m h3user
USER h3user
WORKDIR /app
COPY ./target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
