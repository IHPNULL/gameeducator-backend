# Build context: pasta gateway/ (ver docker-compose.yml da raiz)
#   build   -> compila e roda os testes do gateway
#   runtime -> so o jar + JRE 21

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src/ src/
RUN mvn -B -q package

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=build /app/target/gameeducator-gateway-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
