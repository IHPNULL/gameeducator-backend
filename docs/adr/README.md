# ADRs do B1

Decisões do Architecture Lab B1. O raciocínio completo (RF → RNF → ASR + RPC → alternativas → ADR → C4 → evidência)
está em [`../arquitetura/README.md`](../arquitetura/README.md). As decisões da AC1 (ADR-001 a ADR-019) continuam
valendo, exceto onde um ADR-B1 indica que as substitui.

| ADR | Decisão | Desafio | Status |
|---|---|---|---|
| [ADR-B1-001](ADR-B1-001-monolito-modular.md) | Monólito modular (sem decompor em serviços) | 1 | Aceito |
| [ADR-B1-002](ADR-B1-002-hexagonal-na-gamificacao.md) | Ports & Adapters só no módulo de gamificação | 2 | Aceito |
| [ADR-B1-003](ADR-B1-003-servico-python-de-ia.md) | Serviço Python de IA independente + degradação no Core | 3 | Aceito |
| [ADR-B1-004](ADR-B1-004-eventos-outbox-rabbitmq.md) | Síncrono para o imediato; outbox + RabbitMQ para o eventual | 4 | Aceito |
| [ADR-B1-005](ADR-B1-005-iot-mqtt.md) | MQTT no mesmo RabbitMQ + ACL idempotente | 5 | Aceito |
| [ADR-B1-006](ADR-B1-006-cicd-e-observabilidade.md) | Pipeline por componente + métricas de evidência | 6 | Aceito |

Modelo de um novo ADR: Contexto → ASRs/RPCs → Alternativas → Decisão → Consequências/trade-offs → Evidência.
