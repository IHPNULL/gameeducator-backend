# Build context: repo root (see docker/docker-compose.yml)
#
# Estagios:
#   deps    -> baixa as dependencias do Maven (camada cacheavel)
#   test    -> compila + roda "./mvnw verify" (testes JUnit + gate do JaCoCo).
#              Nao faz parte da imagem final: a pipeline builda so este estagio
#              (--target test) e roda com "docker run" para poder extrair os
#              relatorios mesmo quando um teste falha.
#   build   -> empacota o jar de runtime (sem rodar os testes de novo: ja
#              rodaram no estagio "test")
#   runtime -> ultimo estagio (o "docker build" sem --target usa este): igual
#              ao Dockerfile original, so o jar + JRE

# ---- Dependencies ----
FROM eclipse-temurin:21-jdk AS deps
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

# ---- Test ----
FROM deps AS test
COPY src/ src/
CMD ["./mvnw", "-B", "clean", "verify"]

# ---- Build ----
FROM deps AS build
COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

# ---- Runtime ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=build /app/target/grupo_6_gameeducator-0.0.1-SNAPSHOT.jar app.jar
ENV SPRING_PROFILES_ACTIVE=postgres
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
