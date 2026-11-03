# 🏛️ GameEducator — Architecture Specification & Visual Map

> 💡 **Notion Import Ready**: This document is formatted for direct import into Notion (via *Settings & Members → Import → Markdown* or pasting directly). All diagrams use standard `mermaid` blocks supported natively by Notion's `/mermaid` viewer.

---

### 📋 Page Properties

| Property | Value |
|---|---|
| **System** | GameEducator Platform |
| **Architectural Style** | Polyrepo + Modular Monolith (Core) + Satellite Services (AI, IoT, SPA) |
| **Status** | 🟢 Production / Lab B1 Complete |
| **Last Updated** | November 2026 |
| **Core Repositories** | `gameeducator-backend` · `gameeducator-frontend` · `gameeducator-ia` · `gameeducator-automation` |
| **Test Coverage** | Backend: 95% lines / 90% branches (JaCoCo) · Frontend: 95% lines (Vitest v8) |

---

## 📌 Executive Summary

GameEducator is a gamified learning platform where teachers publish educational content structured into **Missions** and **Challenges** (quizzes/exercises). Students engage with challenges, earn **XP** and unlock **Medals**, compete on class **Rankings**, interact in a peer **Forum**, consult an **AI Assistant (RAG)**, and participate in physical **IoT Laboratories**.

> 🎯 **Core Architectural Philosophy**
> - **Modular Monolith for Core Business Logic**: 1 deployable, 1 database, local ACID transactions for XP/medals/attempts.
> - **Satellite Services for Specialized Loads**: Python FastAPI for AI/ML with independent lifecycle and scaling; RabbitMQ broker with MQTT for unstable edge IoT devices.
> - **Guaranteed Delivery (Transactional Outbox)**: Non-critical side-effects (push notifications, telemetry analytics) are decoupled from HTTP request cycles.

---

## 🗺️ 1. C4 Container Diagram (System Overview)

```mermaid
flowchart TB
    aluno(["👨‍🎓 Aluno"])
    prof(["👨‍🏫 Professor"])
    dev["📟 Dispositivos IoT (MQTT QoS 1)"]

    subgraph ge["GameEducator Platform"]
        spa["💻 SPA Frontend (Vue 3 + Vuetify 2026, :5173 / :8081)"]
        core["⚙️ Core Backend (Spring Boot 4, Java 21, :8080)<br/><i>Monólito Modular</i>"]
        db[("🗄️ PostgreSQL 16 (:5432)<br/>dados + outbox + iot")]
        ia["🧠 Serviço de IA (FastAPI, Python 3.12, :8000)<br/><i>Stateless / RAG + Recomendação</i>"]
        mq[["📬 RabbitMQ 4.1 (:5672 AMQP, :1883 MQTT)<br/><i>Topic Exchange + Filas Quorum</i>"]]
        notif["🔔 Notificação (Python Consumer, :8085)"]
        obs["📊 Observabilidade (Prometheus :9090 + Grafana :3000)"]
    end

    llm["🤖 LLM Local / Remoto (Ollama)"]
    analytics["📈 Analytics Externo (Futuro)"]

    aluno -->|HTTPS| spa
    prof -->|HTTPS| spa
    spa -->|"REST API (JSON + X-Usuario-Id)"| core
    core -->|JDBC / JPA| db
    core -->|"HTTP REST (Timeout + Disjuntor)"| ia
    ia -.->|HTTP| llm
    core -->|"Relay Outbox (AMQP + Confirm)"| mq
    dev -->|"MQTT QoS 1 + LWT"| mq
    mq -->|"core.iot.eventos (AMQP)"| core
    mq -->|"notificacao.eventos (AMQP)"| notif
    mq -.->|"analytics.eventos (AMQP)"| analytics
    obs -.->|"Scrape métricas"| core & ia & mq & notif
```

---

## 🧩 2. Internal Core Architecture (Modular Monolith & Hexagonal)

Inside the single deployable Spring Boot Core (`org.facens.grupo_6_gameeducator`), boundaries are strictly enforced via **ArchUnit** tests:

```mermaid
flowchart TB
    spa["💻 SPA Vue 3 Client"] --> web

    subgraph core["Core — org.facens.grupo_6_gameeducator"]
        web["<b>web & web/dto</b><br/>REST Controllers: Jogo, Missao, Nota, Forum, Ia, Lab<br/>RestExceptionHandler"]
        service["<b>service & service/dto</b><br/>JogoService · MissaoService · NotaService · ForumService"]
        repo["<b>repository</b><br/>Spring Data JPA"]
        domain["<b>domain</b><br/>Entidades JPA: Usuario, Curso, Missao, Desafio, Tentativa, Nota"]

        subgraph gam["🛡️ Módulo Gamificação (Hexagonal / Ports & Adapters)"]
            gapi["GamificacaoApi<br/>(Única porta pública visível)"]
            gapp["aplicacao<br/>Casos de Uso: calcularXp, concederMedalhas"]
            gdom["dominio<br/>ProgressoAluno, PoliticaDeXp, PoliticaDeMedalhas"]
            gport["porta<br/>Interfaces de Repositório puras"]
            gadp["adaptador<br/>Implementações JPA / Em Memória"]

            gapi --> gapp --> gdom
            gapp --> gport
            gadp -.->|implementa| gport
        end

        subgraph ev["📬 Módulo Eventos (Transactional Outbox)"]
            outbox["outbox_evento (Tabela) + Relay Agendado"]
            evad["adaptador: RabbitMQ EventPublisher"]
            outbox --> evad
        end

        subgraph iotmod["📟 Módulo IoT (Camada Anti-Corrupção)"]
            iotad["ConsumidorIotRabbit ➔ TradutorDeEventoIot ➔ RegistroDeEventosIot"]
        end

        subgraph iamod["🧠 Módulo IA (Cliente HTTP Resiliente)"]
            iaad["ClienteHttpIa (Timeout + Circuit Breaker + Fallback)"]
        end

        web --> service --> repo --> domain
        service -->|somente pela API pública| gapi
        service -->|mesma transação ACID| outbox
        service --> iaad
        iotad --> outbox
    end

    repo & gadp & outbox & iotad --> db[("🗄️ PostgreSQL")]
    iaad -->|HTTP :8000| iasvc["Serviço IA"]
    evad -->|AMQP :5672| mq[["RabbitMQ"]]
    mq -->|AMQP| iotad
```

<details>
<summary><b>🔍 Detalhes da Separação Modular (Clique para expandir)</b></summary>

- **Gamificação Isolada (ADR-B1-002)**: As regras de cálculo de XP e medalhas não dependem de Spring Data ou banco de dados. Os casos de uso e o domínio utilizam a porta de repositório, permitindo execução de testes unitários em 3 ms sem inicialização do contexto Spring.
- **ArchUnit Enforcement**: Qualquer injeção indevida ou dependência direta entre camadas é detectada e interrompe o build Maven (`./mvnw test`).
</details>

---

## ⚡ 3. Critical Flow: Challenge Response & Transactional Outbox

This sequence illustrates how GameEducator guarantees instant student feedback while maintaining zero data loss across external consumers:

```mermaid
sequenceDiagram
    autonumber
    actor A as 👨‍🎓 Aluno
    participant S as 💻 SPA Frontend
    participant C as ⚙️ Core Backend (JogoService)
    participant DB as 🗄️ PostgreSQL
    participant R as 🔄 Outbox Relay (Scheduler)
    participant MQ as 📬 RabbitMQ Broker
    participant N as 🔔 Notificação (Consumer)

    A->>S: Clica na resposta do Desafio
    S->>C: POST /api/desafios/{id}/respostas
    Note over C,DB: Transação Local Única (ACID)
    C->>DB: 1. INSERT tentativa (histórico)
    C->>DB: 2. UPDATE progresso_aluno (+XP)
    C->>DB: 3. INSERT medalha_aluno (se atingiu marco)
    C->>DB: 4. INSERT outbox_evento (MissaoConcluida / Medalha)
    C->>DB: COMMIT
    C-->>S: HTTP 200 { correta: true, xpGanho: 50, medalhas: [50] }
    S-->>A: Feedback imediato na tela (< 40ms)

    Note over R,MQ: Processamento Assíncrono Desacoplado
    loop A cada 2 segundos
        R->>DB: SELECT * FROM outbox_evento WHERE publicado_em IS NULL
        R->>MQ: PUBLISH evento (AMQP persistente + publisher confirm)
        alt Broker confirmou recebimento
            R->>DB: UPDATE outbox_evento SET publicado_em = NOW()
        else Broker temporariamente fora
            R->>DB: UPDATE outbox_evento SET tentativas++, ultimo_erro = erro
        end
    end

    MQ-->>N: Entrega evento (notificacao.eventos)
    N->>MQ: Manual ACK após processamento idempotente
```

---

## 📱 4. Frontend Architecture (Vue 3 + 2026 Modern Design)

The frontend SPA was recently upgraded with a 2026 modern design system, full coverage of backend capabilities, and 95% line coverage:

```mermaid
flowchart TD
    subgraph UI["🎨 Vue 3 SPA Architecture (gameeducator-frontend)"]
        app["App.vue (TopBar Glassmorphism + Theme Toggle + Persona Switcher)"]

        subgraph Views["Telas da Aplicação (/src/views)"]
            v1["MissoesView.vue<br/><i>Hero Gamificado, Desafios, Histórico de Tentativas</i>"]
            v2["NovaMissaoView.vue<br/><i>Criação de Missões com Desafios</i>"]
            v3["RankingView.vue<br/><i>Pódio Top 3, Leaderboard, Quadro de Medalhas</i>"]
            v4["NotasView.vue<br/><i>Boletim do Aluno, Média Geral, Status</i>"]
            v5["PainelProfessorView.vue<br/><i>KPIs da Turma, Acompanhamento, Modal Lançar Nota</i>"]
            v6["ForumView.vue<br/><i>Threads de Dúvidas, Perguntas e Respostas</i>"]
            v7["AssistenteView.vue ⭐<br/><i>Chat RAG, Citações de Fontes, Recomendações IA</i>"]
            v8["LaboratorioView.vue ⭐<br/><i>Telemetria IoT em Tempo Real, Eventos de Bancada</i>"]
        end

        subgraph Composables["Lógica Reativa (/src/composables)"]
            c1["useMissoes"]
            c2["useRanking"]
            c3["useMedalhas"]
            c4["useNotas"]
            c5["useDesempenhoTurma"]
            c6["useForum"]
            c7["useIa"]
            c8["useLaboratorio"]
        end

        subgraph API["Cliente HTTP (/src/api/client.js)"]
            client["fetch wrapper com injeção automática de X-Usuario-Id"]
        end

        app --> Views
        v1 --> c1
        v2 --> c1
        v3 --> c2 & c3
        v4 --> c4
        v5 --> c5
        v6 --> c6
        v7 --> c7
        v8 --> c8
        Composables --> client
    end
```

---

## 🚀 5. Deployment & CI/CD Topology

```mermaid
flowchart LR
    subgraph devops["Local Jenkins Pipeline (/src mount)"]
        jb["Job: gameeducator-backend<br/><i>JaCoCo (95%) + ArchUnit + PMD</i>"]
        jf["Job: gameeducator-frontend<br/><i>Vitest (95%) + Vite Build</i>"]
        ji["Job: gameeducator-ia<br/><i>Pytest (90%) + Contract Smoke</i>"]
        ja["Job: gameeducator-automation<br/><i>Cucumber API & Playwright UI</i>"]
    end

    subgraph runtime["Docker Compose Environment (labs/docker-compose.yml)"]
        c_fe["frontend (Nginx :8081 / dev :5173)"]
        c_be["backend (Spring Boot :8080)"]
        c_ia["ia (FastAPI Python :8000)"]
        c_db[("postgres:16 :5432")]
        c_mq[["rabbitmq:4.1 (:5672, :1883, :15672)"]]
        c_notif["notificacao (:8085)"]
        c_prom["prometheus (:9090)"]
        c_graf["grafana (:3000)"]
    end

    jb -->|"Docker Image"| c_be
    jf -->|"Docker Image"| c_fe
    ji -->|"Docker Image"| c_ia
    jb & jf -.->|"Trigger on Success"| ja
    c_fe --> c_be
    c_be --> c_db & c_mq & c_ia
    c_notif --> c_mq
    c_prom -.->|"Metrics Scrape"| c_be & c_ia & c_mq & c_notif
    c_graf --> c_prom
```

---

## 📊 6. Architectural Decision Records (ADRs) Summary

| ADR ID | Decision Summary | Rationale & Trade-offs | Impacted Modules |
|---|---|---|---|
| **ADR-001** | Polyrepo Strategy | Independent build pipelines & toolchains per technology stack (Java, Node, Python). | Workspace structure |
| **ADR-002** | Core Modular Monolith | Single deployable with explicit internal package boundaries rather than microservices network overhead. | `gameeducator-backend` |
| **ADR-B1-001** | High-performance Ranking Index | Compound index `ix_progresso_curso_xp` on PostgreSQL; sub-millisecond leaderboard query. | Database schema |
| **ADR-B1-002** | Hexagonal Architecture for Gamification | Ports & Adapters isolating XP and medal rules from persistence framework. | `gamificacao` module |
| **ADR-B1-003** | Satellite Python AI Service | FastAPI service dedicated to Python-native ML ecosystem with circuit-breaker fallback. | `gameeducator-ia` |
| **ADR-B1-004** | Transactional Outbox + RabbitMQ | Decoupled event publishing with zero message loss and local atomic database commits. | `eventos` module |
| **ADR-B1-005** | IoT Integration via MQTT + ACL | Edge devices communicate over lightweight MQTT 3.1.1 QoS 1; translated via Anti-Corruption Layer. | `iot` module |
| **ADR-B1-006** | Prometheus & Grafana Observability | Comprehensive metrics scraping across Core, AI, Message Broker, and Notification services. | Lab infrastructure |

---

### 📥 How to Import this Page into Notion
1. Open your Notion workspace.
2. Click **Import** in the sidebar and choose **Markdown & CSV**.
3. Select this markdown file (`NOTION_GAMEEDUCATOR_ARCHITECTURE.md`).
4. Notion will automatically convert the headers, property tables, callout blocks, and render all Mermaid diagrams interactively!
