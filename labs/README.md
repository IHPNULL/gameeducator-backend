# Architecture Lab B1 — mini-labs e infraestrutura

Evidências dos 6 desafios descritos em [`docs/arquitetura/README.md`](../docs/arquitetura/README.md).
Todos os scripts rodam a partir da **raiz do `gameeducator-backend`** e gravam o resultado em
`docs/arquitetura/evidencias/desafio-N/`.

| Desafio | Script | Precisa de | Hipótese testada |
|---|---|---|---|
| 1 | `desafio-1/analisar_dependencias.py` | JDK 21 (`jdeps`) | Modularizar a gamificação zera o acoplamento de fora aos internos dela |
| 1 | `desafio-1/provar-fitness-function.sh` | JDK 21 | A fronteira do módulo é verificada: uma violação quebra o build |
| 1 | `desafio-1/benchmark-ranking.sh` | Docker | O ranking com 100 mil usuários não exige serviço separado |
| 2 | `desafio-2/relatorio-adaptadores.sh` | JDK 21 | A mesma regra roda com adaptador JPA e em memória |
| 3 | `desafio-3/executar.sh` | stack | IA lenta/parada não derruba o Core (timeout + disjuntor + fallback) |
| 4 | `desafio-4/executar.sh` | stack | Consumidor/broker fora: síncrono falha, assíncrono conclui e nada se perde |
| 5 | `desafio-5/executar.sh` | stack | Queda + duplicados + inválido: cada evento único entra exatamente uma vez |
| 6 | `desafio-6/executar.sh` | stack | Cada degradação aparece na métrica do ASR e dispara o alerta |

## Stack do lab (Desafios 3 a 6)

```bash
docker compose -f labs/docker-compose.yml up -d --build      # ~3-5 min na primeira vez
docker compose -f labs/docker-compose.yml ps
docker compose -f labs/docker-compose.yml down -v              # derruba e apaga volumes
```

O repositório `gameeducator-ia` precisa estar ao lado deste (mesma convenção do Jenkins local).
Usa as portas 8080/5432 como os outros composes: não suba dois stacks ao mesmo tempo.

| Serviço | URL | Observação |
|---|---|---|
| Core | http://localhost:8080 (Swagger em `/swagger-ui/index.html`, métricas em `/actuator/prometheus`) | perfis `postgres,dev` (Ana=1, Bruno=2, curso=1) |
| IA | http://localhost:8000/docs | `PUT /admin/atraso` habilitado só no lab |
| RabbitMQ | http://localhost:15672 (gameeducator/gameeducator) · AMQP 5672 · MQTT 1883 | topologia em `infra/rabbitmq/definitions.json` |
| Notificação (simulada) | http://localhost:8085/notificacoes | consumidor AMQP + endpoint HTTP do modo síncrono |
| Prometheus | http://localhost:9090 (alertas em `/alerts`) | `infra/prometheus/` |
| Grafana | http://localhost:3000 (anônimo; admin/admin) | dashboard "GameEducator B1 - Evidencias arquiteturais" |

Simulador IoT avulso: `docker compose -f labs/docker-compose.yml run --rm simulador-iot --host rabbitmq --help`.
Modo síncrono (só para comparar no D4): `EVENTOS_MODO=sincrono docker compose -f labs/docker-compose.yml up -d backend`.

| Pasta | Conteúdo |
|---|---|
| `infra/rabbitmq/` | plugins (MQTT, Prometheus), `rabbitmq.conf`, topologia e usuários (`definitions.json`) |
| `infra/prometheus/` | scrape (Core, IA, RabbitMQ, notificação) e regras de alerta ligadas aos ASRs |
| `infra/grafana/` | datasource e dashboard provisionados |
| `notificacao-consumidor/` | serviço de notificação simulado (Python) |
| `simulador-iot/` | dispositivo simulado (Python, paho-mqtt) |
| `comum/funcoes.sh` | funções compartilhadas pelos scripts |
