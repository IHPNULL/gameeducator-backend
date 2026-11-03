# GameEducator - Backend

API REST do GameEducator, plataforma de ensino gamificada (cursos, missões, desafios, XP, notas e fórum).
O frontend fica no repositório [gameeducator-frontend](https://github.com/IHPNULL/gameeducator-frontend).

**Stack:** Java 21 · Spring Boot 4 (Web + Data JPA) · H2 / PostgreSQL · JUnit 5 · JaCoCo

**Integrantes (Grupo 6):** Italo Haas Pascoli (190294) · Maria Eduarda Mota Zandonade (200953)

## Setup local (4 repositórios lado a lado)

```
gameeducator/                 # pasta raiz do projeto
├── gameeducator-backend      # API REST Core + Jenkins local (jenkins/) + Mini-labs (labs/)
├── gameeducator-frontend     # SPA Web (Vue 3, Vuetify, estacao de trabalho utilitaria anos 90)
├── gameeducator-ia           # Servico de IA Python (FastAPI, recomendacao e assistente RAG)
└── gameeducator-automation   # Suites E2E Cucumber (API REST-assured + UI Playwright)
```

```bash
mkdir gameeducator && cd gameeducator
for r in backend frontend ia automation; do git clone https://github.com/IHPNULL/gameeducator-$r.git; done
```

## Arquitetura (B1)

Registro arquitetural do Architecture Lab B1 (6 desafios, matriz de ASRs, ADRs, C4 e evidências):
[`docs/arquitetura/README.md`](docs/arquitetura/README.md). Mini-labs e stack do lab (RabbitMQ/MQTT, IA, Prometheus, Grafana): [`labs/`](labs/README.md).
O serviço de IA (Python/FastAPI) fica no repositório [gameeducator-ia](https://github.com/IHPNULL/gameeducator-ia).

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

## Automação

As suítes Cucumber (API e UI) ficam no repositório [gameeducator-automation](https://github.com/IHPNULL/gameeducator-automation). Este repositório fornece o perfil Spring `automation` (`AutomationSeedData`) que elas usam.

## CI / CD

`Jenkinsfile`: build multi-stage, testes unitários e de integração (161 testes + ArchUnit), análise estática com PMD, validação com Postgres e estágio de Fake Deploy para homologação simulada. Jenkins local (com os 4 jobs encadeados: backend, frontend, automação e IA): `docker compose -f jenkins/docker-compose.yml up -d --build` e abra http://127.0.0.1:8090 (admin/admin). Veja [`jenkins/README.md`](jenkins/README.md).

O planejamento ATDD (BDD → TDD RED/GREEN/BLUE) e a rastreabilidade cenário → service → teste estão em
[`BACKLOG.md`](BACKLOG.md).
