# ADR-B1-006: Pipelines Jenkins por componente com gates arquiteturais + métricas Prometheus que validam os ASRs

**Status:** Aceito · **Desafio:** 6 · **Data:** 2026-10 · **Evolui:** ADR-016 e ADR-017 da AC1

## Contexto

Depois dos Desafios 1–5, a solução tem: Core Java (monólito modular), serviço Python de IA, RabbitMQ (AMQP +
MQTT), consumidor de notificação, PostgreSQL e o SPA. A arquitetura só é sustentável se cada parte puder ser
**testada, implantada e observada** com segurança.

## ASRs e RPCs

| ASR | Como altera a arquitetura |
|---|---|
| **ASR-15 Observabilidade orientada a decisões** (FURPS+ *Supportability*) | Muda a **operação**: cada ASR anterior precisa de uma métrica que diga se a decisão está funcionando em produção, não só se o processo está de pé. |
| **ASR-16 Deploy independente e seguro** (FURPS+ *Supportability*) | Muda a **implantação**: cada componente com pipeline próprio e com *gates* que bloqueiam deploy quebrado, incluindo regras de arquitetura. |
| **ASR-17 Recuperabilidade operacional** (FURPS+ *Reliability*) | Muda a **operação**: condições de degradação (IA fora, broker fora, fila crescendo) precisam virar alertas versionados. |

RPCs: Jenkins é a base da esteira; Prometheus e Grafana são as ferramentas do laboratório; artefatos e
configurações versionados (RPC-11).

## Respostas do Tech Lead

- **Serviços independentes exigem pipelines independentes?** Sim, quando o ASR é deploy independente (ASR-07 e
  ASR-16). Um pipeline único faria a mudança de um modelo de IA esperar e rodar os testes do Core (e vice-versa).
  O que **não** se separa: o teste ponta a ponta (`gameeducator-automation`), que continua disparado pelos
  pipelines de backend e frontend.
- **O que bloqueia deploy?**
  - Core: testes unitários e de aceitação; **ArchUnit** (fronteiras dos ADR-B1-001/002/003/004/005); gate JaCoCo
    95%/90%; integração com Postgres; smoke de `/actuator/prometheus` com as métricas de evidência.
  - IA: pytest com gate de 90%; build da imagem de runtime; **smoke do contrato** `/recomendacoes` na imagem que
    será publicada.
- **Quais métricas validam ASRs anteriores?**

| ASR | Métrica (Prometheus) | O que prova |
|---|---|---|
| ASR-02 ranking | `http_server_requests_seconds` p95 de `/api/cursos/{cursoId}/ranking` | Ranking dentro do SLO sem serviço separado |
| ASR-06 IA isolada | `gameeducator_ia_chamadas_total{resultado}`, `gameeducator_ia_disjuntor_aberto`, `gameeducator_ia_latencia_seconds` | IA fora vira fallback rápido, não erro nem lentidão no Core |
| ASR-09/10 eventos | `gameeducator_outbox_pendentes`, `rabbitmq_queue_messages_ready{queue}`, `notificacoes_recebidas_total` | Falha do broker ou do consumidor acumula e drena sem perda |
| ASR-12 IoT | `gameeducator_iot_eventos_total{resultado=aceito\|duplicado\|invalido}` | Duplicados neutralizados, inválidos isolados |
| Disponibilidade | `up{job}` e `http_server_requests_seconds_count{status=~"5.."}` | Quem caiu e qual o impacto no usuário |

- **Métrica técnica × evidência arquitetural:** "CPU 40%" ou "heap 300 MB" são métricas técnicas: dizem se o
  processo está saudável. **Evidência arquitetural** é uma métrica ligada a um ASR e a uma hipótese da decisão
  ("com a IA fora, `resultado=disjuntor_aberto` cresce e a p95 de `/recomendacoes` continua baixa"). Ela é coletada
  sob uma condição provocada e comparada com o comportamento esperado no ADR.

## Alternativas

| Alternativa | ASR-16 | Custo | Veredito |
|---|---|---|---|
| Pipeline único para todos os componentes | Deploy acoplado: mudança na IA espera os testes do Core | Menor (1 Jenkinsfile) | Rejeitada |
| **Pipeline por componente (repo) + E2E encadeado** | **Cada componente publica sozinho; E2E protege a integração** | Mais Jenkinsfiles, padrão comum | **Escolhida** |

| Alternativa de observabilidade | Veredito |
|---|---|
| Logs apenas | Rejeitada: não mostra tendência nem dispara alerta |
| **Métricas Prometheus (pull) + Grafana + regras de alerta versionadas** | **Escolhida** (RPC-11); tracing distribuído (OpenTelemetry) fica como próximo passo |

## Decisão

- **Jenkins:** jobs `gameeducator-backend`, `-frontend`, `-automation` (AC1) **+ `gameeducator-ia`**, criados por
  `jenkins/init.groovy.d/create-job.groovy`. Cada repositório tem seu `Jenkinsfile` com o mesmo padrão
  (teste na imagem → imagem de runtime → smoke → publish em `main`).
- **Core:** o stage `Integracao (Postgres)` ganha um smoke de observabilidade: falha se `/actuator/health` não estiver UP ou se `/actuator/prometheus` não
  expuser as métricas de evidência.
- **Observabilidade:** Actuator + Micrometer Prometheus no Core, `prometheus-client` na IA e no consumidor, e
  plugin `rabbitmq_prometheus` no broker. Configuração versionada em `labs/infra/prometheus` (scrape + alertas) e
  `labs/infra/grafana` (datasource + dashboard "GameEducator B1 - Evidências arquiteturais").

## Consequências

- **Mais fácil:** saber se uma decisão continua valendo em execução; publicar a IA sem tocar no Core; regras de
  arquitetura não regridem sem quebrar o build.
- **Mais difícil:** mais pipelines e mais configuração para manter; métricas customizadas exigem nomes estáveis
  (são contrato com os dashboards).
- **Pendente:** tracing distribuído (Core → IA → broker) e SLOs formais com *error budget*.

## Evidência

`docs/arquitetura/evidencias/desafio-6/`: execução dos pipelines, condições provocadas (IA fora, consumidor fora,
broker fora) e o efeito registrado no Prometheus/Grafana.
