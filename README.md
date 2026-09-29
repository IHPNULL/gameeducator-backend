# GameEducator - Backend

API REST do GameEducator, plataforma de ensino gamificada (cursos, missões, desafios, XP, notas e fórum).
O frontend fica no repositório [gameeducator-frontend](https://github.com/IHPNULL/gameeducator-frontend).

**Stack:** Java 21 · Spring Boot 4 (Web + Data JPA) · H2 / PostgreSQL · JUnit 5 · JaCoCo

**Integrantes (Grupo 6):** Italo Haas Pascoli (190294) · Maria Eduarda Mota Zandonade (200953)

## Como rodar

```bash
./mvnw spring-boot:run     # API em http://localhost:8080 (H2 em memória)
./mvnw clean test          # testes
./mvnw clean verify        # testes + relatório e quality gate do JaCoCo (95% linhas / 90% branches)
./mvnw test -Dtest=AlunoVeNotasTest   # uma classe de teste
```

Relatório de cobertura: `target/site/jacoco/index.html`. Swagger UI em `/swagger-ui/index.html`.

Perfis Spring: padrão (H2), `dev` (dados de exemplo), `postgres` (PostgreSQL via `DB_*`),
`automation` (H2 com dados fixos para a suíte Cucumber).

## Docker

```bash
docker compose -f docker/docker-compose.yml up --build    # Postgres + backend (:8080) + pgAdmin (:5050)
```

## Automação de API (Cucumber-JVM + REST-assured)

```bash
docker compose -f docker/docker-compose.yml -f docker/docker-compose.automation.yml up -d --build backend
cd api-tests && ./mvnw test        # -Dautomation.baseUrl=http://outro:porta para outro host
docker compose -f docker/docker-compose.yml -f docker/docker-compose.automation.yml down -v
```

Relatório: `api-tests/target/cucumber-report/cucumber-report.html`.

## CI

`Jenkinsfile` builda a imagem, roda os testes dentro dela, valida a integração com Postgres e roda a suíte
Cucumber de API. Jenkins local: veja [`jenkins/README.md`](jenkins/README.md).

O planejamento ATDD (BDD → TDD RED/GREEN/BLUE) e a rastreabilidade cenário → service → teste estão em
[`BACKLOG.md`](BACKLOG.md).
