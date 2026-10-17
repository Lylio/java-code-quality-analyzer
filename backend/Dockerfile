FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B verify
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 app
COPY --from=build /app/target/java-code-quality-analyzer-1.0.0.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java","-Xmx512m","-jar","app.jar"]
