# GameEducator - Backend

API REST do GameEducator, plataforma de ensino gamificada (cursos, missões, desafios, XP, notas e fórum).
O frontend fica no repositório [gameeducator-frontend](https://github.com/IHPNULL/gameeducator-frontend).

**Stack:** Java 21 · Spring Boot 4 (Web + Data JPA) · H2 / PostgreSQL · JUnit 5 · JaCoCo

**Integrantes (Grupo 6):** Italo Haas Pascoli (190294) · Maria Eduarda Mota Zandonade (200953)

## Executar a plataforma completa (AF)

Na raiz deste repositório, um único comando sobe tudo:

```bash
docker compose up --build
```

Abra **http://localhost:8080**. O API Gateway (Spring Cloud Gateway, pasta [`gateway/`](gateway/)) é a única porta de entrada:

| Caminho no gateway | Destino | Container |
|---|---|---|
| `/` (e qualquer rota da SPA) | Frontend Vue 3 servido pelo Nginx | `frontend` |
| `/api/**` | API REST do Core | `backend` |
| `/swagger-ui.html`, `/v3/api-docs` | Documentação OpenAPI do Core | `backend` |
| `/ia/saude` | Saúde do serviço de IA | `ia` (FastAPI) |
| `/actuator/gateway/routes` | Rotas ativas do gateway | `gateway` |

O gateway também carimba um `X-Correlation-Id` em toda requisição e o devolve na resposta. Core, IA, PostgreSQL e o consumidor de notificação não publicam portas: ficam só na rede interna do compose. As outras portas publicadas são 15672 (RabbitMQ UI, `gameeducator`/`gameeducator`), 9090 (Prometheus) e 3000 (Grafana).

- O frontend e a IA são buildados direto do branch `main` dos repositórios no GitHub, então basta clonar este repositório. Para buildar a partir dos clones locais, copie `.env.example` para `.env`.
- Se a porta 8080 estiver ocupada, use `GATEWAY_PORT=8088 docker compose up --build`.
- Para parar e apagar os dados: `docker compose down -v`.

## Arquitetura C4

Documentação da arquitetura no modelo C4, do nível mais amplo ao mais detalhado: **1. Contexto → 2. Containers → 3. Componentes → 4. Código**, mais a visão de **implantação** (limites do `docker compose`).

**Legenda:** 🟦 azul escuro = pessoa · 🟦 azul = container/componente existente · ⬜ cinza = sistema externo · 🟨 tracejado amarelo = **a implementar na AF** (requisitos novos; atualizar a legenda quando estiver pronto) · linha cheia = síncrono (REST/HTTP) · linha tracejada = assíncrono (AMQP/MQTT) ou opcional.

### Nível 1 — Contexto do sistema

Quem usa o GameEducator e com que sistemas externos ele conversa.

```mermaid
flowchart TB
    aluno["<b>Aluno / Colaborador</b><br/>[Pessoa]<br/>Resolve missões, acompanha XP, medalhas,<br/>notas e recomendações; usa o assistente"]:::pessoa
    prof["<b>Professor / Gestor</b><br/>[Pessoa]<br/>Cria missões, lança notas,<br/>acompanha o desempenho da turma"]:::pessoa
    ge["<b>GameEducator</b><br/>[Sistema de software]<br/>Plataforma de ensino gamificada com<br/>recomendação (ML), assistente (RAG) e agentes de IA"]:::sistema
    dev["<b>Dispositivos de laboratório</b><br/>[Sistema externo]<br/>Sensores e kits IoT"]:::externo
    llm["<b>Provedor de LLM</b><br/>[Sistema externo, opcional]<br/>Ollama local"]:::externo

    aluno -->|"Usa [HTTPS]"| ge
    prof -->|"Usa [HTTPS]"| ge
    dev -.->|"Publica eventos de laboratório [MQTT]"| ge
    ge -.->|"Gera respostas [HTTP]"| llm

    classDef pessoa fill:#08427b,stroke:#052e56,color:#fff
    classDef sistema fill:#1168bd,stroke:#0b4884,color:#fff
    classDef externo fill:#999999,stroke:#6b6b6b,color:#fff
```

### Nível 2 — Containers

Os processos que compõem o sistema, as tecnologias de cada um e como se comunicam. Todos sobem com `docker compose up --build`.

```mermaid
flowchart TB
    aluno["<b>Aluno / Colaborador</b><br/>[Pessoa]"]:::pessoa
    prof["<b>Professor / Gestor</b><br/>[Pessoa]"]:::pessoa
    dev["<b>Dispositivos de laboratório</b><br/>[Sistema externo]"]:::externo
    llm["<b>Provedor de LLM</b><br/>[Sistema externo, opcional]"]:::externo

    subgraph ge["GameEducator [Sistema]"]
        gw["<b>API Gateway</b><br/>[Container: Java 21, Spring Cloud Gateway]<br/>Porta de entrada única, roteamento,<br/>X-Correlation-Id"]:::container
        spa["<b>SPA</b><br/>[Container: Vue 3 + Vuetify / Nginx]<br/>Interface do aluno e do professor"]:::container
        core["<b>Core</b><br/>[Container: Java 21, Spring Boot 4]<br/>Monólito modular: missões, jogo, gamificação,<br/>notas, fórum, IoT, eventos (outbox)"]:::container
        ia["<b>Serviço de IA</b><br/>[Container: Python 3.12, FastAPI]<br/>Recomendação + assistente RAG, stateless"]:::container
        db[("<b>Banco de dados</b><br/>[Container: PostgreSQL 16]<br/>Dados de negócio, outbox, eventos IoT")]:::container
        mq[["<b>Message broker</b><br/>[Container: RabbitMQ 4.1 + MQTT]<br/>Exchange gameeducator.eventos, filas quorum, DLQs"]]:::container
        notif["<b>Serviço de notificação</b><br/>[Container: Python]<br/>Consumidor idempotente de eventos"]:::container
        obs["<b>Observabilidade</b><br/>[Container: Prometheus + Grafana]<br/>Coleta métricas de todos os containers<br/>[HTTP /metrics], painéis e alertas"]:::container
        agente["<b>Agentes de IA</b><br/>[Componente do Core: Spring AI]<br/>Orientação e acompanhamento,<br/>com autorização do usuário (RF04)"]:::planejado
        treino["<b>Worker de treinamento</b><br/>[Container: Python]<br/>Classificação de competência (RF02),<br/>status do treino (RF05)"]:::planejado
    end

    aluno & prof -->|"Usa [HTTPS]"| gw
    gw -->|"/ [HTTP]"| spa
    gw -->|"/api/** [REST/JSON]"| core
    gw -->|"/ia/saude [HTTP]"| ia
    core -->|"Lê e grava [JDBC]"| db
    core -->|"Recomendar, perguntar [REST/JSON,<br/>timeout + disjuntor]"| ia
    ia -.->|"Gera texto [HTTP]"| llm
    core -.->|"Publica eventos via relay do outbox [AMQP]"| mq
    mq -.->|"notificacao.eventos [AMQP]"| notif
    dev -.->|"gameeducator.lab.* [MQTT]"| mq
    mq -.->|"core.iot.eventos [AMQP]"| core
    core --- agente
    agente -.->|"Usa ferramentas do Core<br/>e o RAG [REST]"| ia
    core -.->|"Pedido de treinamento [AMQP]"| mq
    mq -.->|"ia.treinamento [AMQP]"| treino

    classDef pessoa fill:#08427b,stroke:#052e56,color:#fff
    classDef container fill:#1168bd,stroke:#0b4884,color:#fff
    classDef externo fill:#999999,stroke:#6b6b6b,color:#fff
    classDef planejado fill:#fff4cc,stroke:#b38f00,color:#000,stroke-dasharray:5 5
    style ge fill:none,stroke:#666,stroke-dasharray:5 5
```

| Container | Repositório | Tecnologia | Responsabilidade |
|---|---|---|---|
| API Gateway | `gameeducator-backend/gateway` | Spring Cloud Gateway | Única porta exposta; roteia SPA, API e IA |
| SPA | `gameeducator-frontend` | Vue 3, Vuetify, Nginx | Telas do aluno e do professor |
| Core | `gameeducator-backend` | Spring Boot 4, JPA | Regras de negócio, autorização, outbox |
| Serviço de IA | `gameeducator-ia` | FastAPI | Recomendação e assistente RAG, sem acesso ao banco |
| Banco de dados | — | PostgreSQL 16 | Persistência do Core |
| Message broker | `gameeducator-backend/labs/infra` | RabbitMQ 4.1 (AMQP + MQTT) | Eventos assíncronos e IoT |
| Serviço de notificação | `gameeducator-backend/labs/notificacao-consumidor` | Python, pika | Consome eventos de domínio |
| Observabilidade | `gameeducator-backend/labs/infra` | Prometheus, Grafana | Métricas, painéis e alertas |

**Comunicação entre containers**

| Interação | Estilo | Por quê |
|---|---|---|
| Navegador → Gateway → SPA / Core | Síncrono (REST) | O usuário espera a resposta na tela |
| Core → Serviço de IA (recomendar, perguntar) | Síncrono (REST) com timeout de 2 s, disjuntor e fallback | A resposta é necessária na hora; a IA fora não pode travar o Core (ADR-B1-003) |
| Core → RabbitMQ → Notificação | Assíncrono (AMQP via outbox) | Notificar é eventual; a falha do consumidor não pode desfazer a conclusão da missão (ADR-B1-004) |
| Dispositivos → RabbitMQ → Core | Assíncrono (MQTT → AMQP) | Dispositivos com conectividade intermitente; o broker absorve picos (ADR-B1-005) |
| Core → RabbitMQ → Worker de treinamento 🟨 | Assíncrono (AMQP) | Treinar é demorado; o pedido fica na fila enquanto o ML estiver fora (RNF02) |

### Nível 3 — Componentes

#### Core (Spring Boot)

Monólito modular em camadas (`web → service → repository → domain`), com módulos de fronteira verificada pelo ArchUnit (`ArquiteturaModularTest`). A gamificação é hexagonal (ADR-B1-002); IA, eventos e IoT ficam atrás de portas com adaptadores trocáveis.

```mermaid
flowchart TB
    gw["<b>API Gateway</b><br/>[Container]"]:::container
    ia["<b>Serviço de IA</b><br/>[Container]"]:::container
    db[("<b>PostgreSQL</b><br/>[Container]")]:::container
    mq[["<b>RabbitMQ</b><br/>[Container]"]]:::container

    subgraph core["Core [Container: Spring Boot 4]"]
        web["<b>Controllers REST</b><br/>[web]<br/>Missao, Jogo, Nota, Forum, Turma,<br/>Ia, Laboratorio + RestExceptionHandler"]:::componente
        svc["<b>Serviços de aplicação</b><br/>[service]<br/>MissaoService, JogoService, NotaService,<br/>ForumService: regras e autorização"]:::componente
        iasvc["<b>Serviços de IA do Core</b><br/>[service]<br/>RecomendacaoService, AssistenteService"]:::componente
        gam["<b>Gamificação</b><br/>[módulo hexagonal]<br/>GamificacaoApi: XP, medalhas, ranking"]:::componente
        iaport["<b>Cliente de IA</b><br/>[módulo ia]<br/>ServicoDeIa → ServicoDeIaProtegido<br/>(Disjuntor) → ClienteHttpIa"]:::componente
        evt["<b>Eventos</b><br/>[módulo eventos]<br/>EmissorViaOutbox → OutboxEvento<br/>RelayDoOutbox → PublicadorRabbitMq"]:::componente
        iot["<b>IoT</b><br/>[módulo iot, anticorrupção]<br/>ConsumidorIotRabbit → TradutorDeEventoIot<br/>→ RegistroDeEventosIot"]:::componente
        repo["<b>Repositórios</b><br/>[repository]<br/>Spring Data JPA"]:::componente
        agente["<b>Agente de orientação</b><br/>[Spring AI]<br/>Ferramentas com autorização (RF04)"]:::planejado
    end

    gw -->|"REST/JSON"| web
    web --> svc & iasvc
    svc -->|"só pela API pública"| gam
    svc -->|"emite MissaoConcluida,<br/>MedalhaConquistada"| evt
    iasvc --> iaport
    iaport -->|"REST, timeout + disjuntor"| ia
    svc & gam & evt & iot --> repo
    repo -->|"JDBC"| db
    evt -.->|"AMQP"| mq
    mq -.->|"AMQP"| iot
    web -.-> agente
    agente -.-> svc & iaport

    classDef container fill:#1168bd,stroke:#0b4884,color:#fff
    classDef componente fill:#85bbf0,stroke:#5d82a8,color:#000
    classDef planejado fill:#fff4cc,stroke:#b38f00,color:#000,stroke-dasharray:5 5
    style core fill:none,stroke:#666,stroke-dasharray:5 5
```

#### Serviço de IA (FastAPI)

```mermaid
flowchart TB
    core["<b>Core</b><br/>[Container]"]:::container
    gw["<b>API Gateway</b><br/>[Container]"]:::container
    llm["<b>Provedor de LLM</b><br/>[Sistema externo, opcional]"]:::externo

    subgraph ia["Serviço de IA [Container: FastAPI]"]
        api["<b>API HTTP</b><br/>[app/main.py]<br/>POST /recomendacoes · POST /assistente/perguntas<br/>GET /saude · GET /metrics"]:::componente
        rec["<b>Modelo de recomendação</b><br/>[app/recomendacao/modelo.py]<br/>Pontuação versionada (MODELO_VERSAO)"]:::componente
        ret["<b>Recuperação (R do RAG)</b><br/>[app/assistente/recuperacao.py]<br/>TF-IDF + cosseno sobre o material do curso"]:::componente
        gen["<b>Geração (G do RAG)</b><br/>[app/assistente/geracao.py]<br/>Extrativo (padrão) ou Ollama, com fallback"]:::componente
        met["<b>Métricas</b><br/>[app/metricas.py]<br/>Prometheus"]:::componente
        clf["<b>Classificador de competência</b><br/>[scikit-learn] (RF02)"]:::planejado
    end

    core -->|"REST/JSON"| api
    gw -->|"/ia/saude"| api
    api --> rec & ret & met
    ret --> gen
    gen -.->|"HTTP"| llm
    api -.-> clf

    classDef container fill:#1168bd,stroke:#0b4884,color:#fff
    classDef componente fill:#85bbf0,stroke:#5d82a8,color:#000
    classDef externo fill:#999999,stroke:#6b6b6b,color:#fff
    classDef planejado fill:#fff4cc,stroke:#b38f00,color:#000,stroke-dasharray:5 5
    style ia fill:none,stroke:#666,stroke-dasharray:5 5
```

#### API Gateway (Spring Cloud Gateway)

```mermaid
flowchart LR
    nav["<b>Navegador</b>"]:::pessoa
    subgraph gw["API Gateway [Container]"]
        corr["<b>CorrelacaoFilter</b><br/>[GlobalFilter]<br/>Garante X-Correlation-Id"]:::componente
        rotas["<b>Rotas</b><br/>[application.yml]<br/>core-api · core-openapi · ia-saude · frontend"]:::componente
        act["<b>Actuator</b><br/>/actuator/gateway/routes · health · prometheus"]:::componente
    end
    core["<b>Core</b>"]:::container
    spa["<b>SPA</b>"]:::container
    ia["<b>Serviço de IA</b>"]:::container
    nav --> corr --> rotas
    rotas -->|"/api/**, /v3/api-docs, /swagger-ui"| core
    rotas -->|"/ia/saude"| ia
    rotas -->|"/** (menor prioridade)"| spa

    classDef pessoa fill:#08427b,stroke:#052e56,color:#fff
    classDef container fill:#1168bd,stroke:#0b4884,color:#fff
    classDef componente fill:#85bbf0,stroke:#5d82a8,color:#000
    style gw fill:none,stroke:#666,stroke-dasharray:5 5
```

### Nível 4 — Código: módulo de gamificação (hexagonal)

O núcleo (domínio e aplicação) só conhece as **portas**; os adaptadores JPA e em memória as implementam e são escolhidos por configuração (`app.gamificacao.persistencia=jpa|memoria`), sem mudar o núcleo.

```mermaid
classDiagram
    direction LR
    class GamificacaoApi {
        <<interface>>
        +pontuarResposta(RespostaPontuavel) ResultadoPontuacao
        +xpNoCurso(alunoId, cursoId) int
        +ranking(cursoId) List~PosicaoNoRanking~
        +medalhasDoAluno(alunoId, cursoId) List~MedalhaDoAluno~
    }
    class GamificacaoService
    class PoliticaDeXp {
        +xpACreditar(correta, jaHaviaAcertado, xpDoDesafio)$ int
    }
    class PoliticaDeMedalhas {
        +novasMedalhas(xpAntes, xpDepois, jaConquistadas) List~Integer~
    }
    class Progresso {
        <<record>>
        +creditar(xp) Progresso
    }
    class RepositorioDeProgresso {
        <<interface>>
        +carregar(alunoId, cursoId) Progresso
        +salvar(Progresso)
        +ranking(cursoId) List~PosicaoNoRanking~
    }
    class RepositorioDeMedalhas {
        <<interface>>
        +marcosConquistados(alunoId, cursoId) Set~Integer~
        +registrar(alunoId, cursoId, marcoXp)
        +listar(alunoId, cursoId) List~MedalhaDoAluno~
    }
    class ProgressoJpaAdapter
    class MedalhaJpaAdapter
    class GamificacaoEmMemoria
    class JogoService

    JogoService --> GamificacaoApi : usa
    GamificacaoApi <|.. GamificacaoService
    GamificacaoService --> PoliticaDeXp
    GamificacaoService --> PoliticaDeMedalhas
    GamificacaoService --> Progresso
    GamificacaoService --> RepositorioDeProgresso : porta
    GamificacaoService --> RepositorioDeMedalhas : porta
    RepositorioDeProgresso <|.. ProgressoJpaAdapter
    RepositorioDeMedalhas <|.. MedalhaJpaAdapter
    RepositorioDeProgresso <|.. GamificacaoEmMemoria
    RepositorioDeMedalhas <|.. GamificacaoEmMemoria
```

### Implantação — limites do `docker compose`

Um único host Docker, uma rede interna. Só o gateway e as ferramentas de operação publicam portas; Core, IA, banco e notificação ficam inacessíveis de fora.

```mermaid
flowchart TB
    nav["<b>Navegador</b>"]:::pessoa
    subgraph host["Host Docker [docker compose up --build]"]
        subgraph pub["Portas publicadas"]
            gw["gateway :8080"]:::container
            rmqui["RabbitMQ UI :15672"]:::container
            prom["Prometheus :9090"]:::container
            graf["Grafana :3000"]:::container
        end
        subgraph rede["Rede interna do compose (sem porta publicada)"]
            spa["frontend :80"]:::container
            core["backend :8080"]:::container
            ia["ia :8000"]:::container
            db[("db :5432")]:::container
            mq[["rabbitmq :5672"]]:::container
            notif["notificacao :8085"]:::container
        end
    end
    nav --> gw & rmqui & graf
    gw --> spa & core & ia
    core --> db & mq & ia
    mq --> notif

    classDef pessoa fill:#08427b,stroke:#052e56,color:#fff
    classDef container fill:#1168bd,stroke:#0b4884,color:#fff
    style host fill:none,stroke:#666,stroke-dasharray:5 5
    style pub fill:none,stroke:#2e7d32
    style rede fill:none,stroke:#999
```

As decisões por trás de cada diagrama estão nos ADRs do B1 ([`docs/adr/`](docs/adr/README.md)) e no registro arquitetural ([`docs/arquitetura/README.md`](docs/arquitetura/README.md)).

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

`Jenkinsfile`: build multi-stage, testes unitários e de integração (161 testes + ArchUnit), análise estática com PMD, validação com Postgres e estágio de deploy em homologação (staging simulado). Jenkins local (com os 4 jobs encadeados: backend, frontend, automação e IA): `docker compose -f jenkins/docker-compose.yml up -d --build` e abra http://127.0.0.1:8090 (admin/admin). Veja [`jenkins/README.md`](jenkins/README.md).

O planejamento ATDD (BDD → TDD RED/GREEN/BLUE) e a rastreabilidade cenário → service → teste estão em
[`BACKLOG.md`](BACKLOG.md).
