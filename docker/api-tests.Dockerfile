# Build context: api-tests/ (ver docker/docker-compose.yml e Jenkinsfile)
#
# Imagem que RODA a suite Cucumber (nivel de API REST) de api-tests/
# contra um backend real, ja de pe no perfil "automation" (ver
# docker/docker-compose.automation.yml). Nao publica nada: e usada so
# pela pipeline do Jenkins para validar o backend construido nesta mesma build.

FROM eclipse-temurin:21-jdk AS deps
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

FROM deps AS runtime
COPY src/ src/
RUN ./mvnw -B test-compile

# http://backend:8080 e o nome do servico backend na rede do Docker Compose.
# Para apontar para outro endereco: docker run <imagem> -Dautomation.baseUrl=http://outro:porta
ENTRYPOINT ["./mvnw", "-B", "test"]
CMD ["-Dautomation.baseUrl=http://backend:8080"]
