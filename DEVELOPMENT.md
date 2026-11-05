# Guia de Desenvolvimento — Backend

Este documento reúne instruções de desenvolvimento, arquitetura, comandos de build, testes e convenções do backend do GameEducator.

Backend do GameEducator: Java 21, Spring Boot 4, Spring Data JPA, H2 (padrão em memória) e PostgreSQL (produção). O frontend em Vue 3 fica no repositório separado `gameeducator-frontend`. Código, comentários e identificadores em português (sem acentos no código). Projeto orientado a ATDD: cenários BDD orientam o ciclo TDD RED/GREEN/BLUE; o arquivo `BACKLOG.md` mapeia cada cenário ao respectivo service e classe de teste.

## Commands

```bash
./mvnw spring-boot:run                       # API on :8080; H2 in memory
./mvnw clean test                            # all tests
./mvnw test -Dtest=AlunoVeNotasTest          # one class (add #metodo for one method)
./mvnw clean verify                          # tests + JaCoCo report + coverage gate + PMD check (JDK 21; PMD 7 breaks on JDK 25)
```
JaCoCo gate (`pom.xml`) fails `verify` below 95% lines / 90% branches; report at `target/site/jacoco/index.html`. Bootstrap/seed/OpenAPI config classes are excluded — add new non-logic config classes to those excludes.

End-to-end Cucumber suites live in the separate `gameeducator-automation` repo and run against this backend in the `automation` profile.

Local Jenkins (`jenkins/`) mounts the folder that contains the sibling repos (`SOURCES_DIR`, default `../..`) at `/src` and creates four jobs from each repo's `Jenkinsfile`: `gameeducator-backend`, `gameeducator-frontend`, `gameeducator-automation`, `gameeducator-ia` (`gameeducator-automation` auto-triggers after backend or frontend succeeds; set in `jenkins/init.groovy.d/create-job.groovy`, which only runs on first boot — use `down -v` after editing it). Sibling checkouts must be named after their repos.

`Jenkinsfile` runs the pipeline (image, tests in image, Postgres integration); local Jenkins in `jenkins/` (see its README). Docker build context is the repo root (`docker/backend.Dockerfile`).

## Architecture

Layers under `org.facens.grupo_6_gameeducator`: `web` (controllers + `web/dto` records) → `service` (all business rules; `service/dto`) → `repository` (Spring Data) → `domain` (JPA entities). Services: `MissaoService`, `JogoService` (answering challenges, XP, medals, ranking), `NotaService`, `ForumService`.

- **No real auth.** The caller is identified by the `X-Usuario-Id` header (`MissaoController.HEADER_USUARIO`, reused by other controllers). Authorization (professor vs. aluno, enrollment via `Matricula`) is enforced in services.
- **Error mapping** is centralized in `web/RestExceptionHandler`: `RecursoNaoEncontradoException`→404, `AcessoNegadoException`→403, `RegraDeNegocioException`→400, plus missing header and bean validation → 400. Throw these from services.
- **Profiles:** default = H2 (`ddl-auto=update`); `dev` seeds `config/DadosDeExemplo`; `postgres` = persistent DB via `DB_*` env vars; `automation` = H2 `create-drop` seeded by `config/AutomationSeedData` (the Cucumber features in the `gameeducator-automation` repo depend on that data — change them together). No Flyway/Liquibase. CORS origins come from `app.cors.allowed-origins` (`WebConfig`).

### Tests
- Acceptance tests (`Aluno*Test`, `Professor*Test`) extend `CenarioBase` (fixtures `umProfessor`, `umAluno`, `umCurso`, `matricular`, `missaoDeExemplo`; `@Transactional`, rolled back per test), call services directly, Arrange/Action/Assert, `@DisplayName` citing the BDD criterion.
- `web/*ControllerTest` are `@WebMvcTest` with mocked services; `CenarioBddIntegracaoTest` is end-to-end `@SpringBootTest`; `CaminhosDeErro*` and `RegrasDeDominioTest` cover error paths/invariants (needed for the coverage gate).

## B1 architecture lab

Architecture record (RF → ASR → ADR → C4 → evidence) lives in `docs/arquitetura/README.md`, ADRs in `docs/adr/`, mini-labs and the lab Docker stack in `labs/`. Module boundaries (`gamificacao`, `ia`, `eventos`, `iot`) are enforced by `arquitetura/ArquiteturaModularTest` (ArchUnit) during `verify` — new code must respect them. Broker features are off by default (`app.mensageria.habilitada=false`), so tests and dev need no RabbitMQ.
