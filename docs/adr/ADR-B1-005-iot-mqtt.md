# ADR-B1-005: Dispositivos IoT via MQTT (QoS 1) no mesmo RabbitMQ, com camada anticorrupção e idempotência no Core

**Status:** Aceito · **Desafio:** 5 · **Data:** 2026-10

## Contexto

Laboratórios passam a produzir eventos de **presença, início e conclusão de atividade (RF06)** a partir de
dispositivos com pouco poder computacional e conexão instável. Os eventos devem entrar na plataforma **sem
acoplar o Core a cada dispositivo**.

## ASRs e RPCs

| ASR | Como altera a arquitetura |
|---|---|
| **ASR-12 Tolerância à conectividade intermitente** (FURPS+ *Reliability*) | Muda a **comunicação**: o protocolo precisa de confirmação de entrega, sessão que sobreviva a quedas e reenvio. O Core precisa ser **idempotente** (os dados ganham uma chave natural `eventoId`). |
| **ASR-13 Eficiência de comunicação** (FURPS+ *Performance*, em dispositivos restritos) | Muda o **protocolo**: conexão persistente, cabeçalho de poucos bytes e payload compacto, em vez de handshake HTTP por evento. |
| **ASR-14 Segurança/isolamento da integração** (FURPS+ *+ (segurança)*) | Muda a **estrutura e a operação**: o dispositivo nunca fala com o Core diretamente; credencial própria e permissão por tópico no broker; payload validado em uma camada anticorrupção. |

Não selecionados como ASR: *interoperabilidade*, que MQTT, padrão de mercado, já cobre; *disponibilidade*, que vem
do ASR-12; e *escalabilidade*, porque dezenas de laboratórios são pouco para qualquer broker MQTT.

RPCs: dispositivos com baixo poder computacional e conectividade intermitente (RPC-09); protocolo e broker
adicionam componentes operacionais (RPC-10); broker do Desafio 4 já em operação (decisão ADR-B1-004).

## Respostas do Tech Lead

- **REST é suficiente?** Para um dispositivo sempre conectado e com folga de CPU, sim. Aqui não é: cada evento
  exigiria TCP + (TLS) + HTTP com cabeçalhos de centenas de bytes, e quem resolve reenvio e buffer em queda é o
  dispositivo. O lab mede o tamanho por evento: MQTT vs. requisição REST mínima.
- **O que o MQTT resolve especificamente?** Conexão persistente e leve; **QoS 1** com `PUBACK` (o dispositivo sabe
  que o broker recebeu); **sessão persistente** (`clean_session=false`); **LWT** (o broker anuncia `offline` se o
  dispositivo cair); tópicos hierárquicos que roteiam por laboratório e dispositivo sem o Core saber disso.
- **Depois do MQTT, há motivo para outro broker?** **Não.** O RabbitMQ do Desafio 4 tem plugin MQTT nativo: as
  publicações MQTT caem no exchange `amq.topic` e são roteadas para a fila AMQP `core.iot.eventos`. Um broker
  dedicado (Mosquitto ou EMQX) + *bridge* para o RabbitMQ seria mais um componente e mais um ponto de falha sem ASR
  que o justifique. **Gatilho:** milhares de conexões simultâneas ou MQTT 5 avançado (ex.: *shared subscriptions*
  em larga escala), que favoreceriam EMQX ou HiveMQ.
- **Como impedir duplicidade ou perda relevante?**
  - *Perda:* QoS 1 (dispositivo → broker), **buffer local** no dispositivo durante a queda, fila *quorum* durável e
    **ack do consumidor só depois de gravar** no banco.
  - *Duplicidade:* QoS 1 é "pelo menos uma vez", e o buffer pode reenviar. O dispositivo gera o `eventoId` (UUID)
    e o Core usa esse id como **chave primária** de `evento_iot`: a segunda entrega vira `DUPLICADO` (métrica) e é
    descartada com ack.
  - *Lixo:* payload fora do contrato → `AmqpRejectAndDontRequeueException` → `core.iot.eventos.dlq`.

## Alternativas

| Alternativa | ASR-12 | ASR-13 | ASR-14 | Componentes novos | Veredito |
|---|---|---|---|---|---|
| (A) REST direto do dispositivo para o Core | Retry e buffer por conta do dispositivo; Core exposto a cada device | Pior (handshake + cabeçalhos por evento) | Core vira gateway de dispositivos | 0 | Rejeitada |
| (B) MQTT (Mosquitto) + consumidor/gateway dedicado | Sim | Sim | Sim | Broker MQTT + gateway | Válida, mas 2 componentes a mais |
| (C) MQTT + bridge para a mensageria interna (Mosquitto → RabbitMQ) | Sim | Sim | Sim | Broker MQTT + bridge | Rejeitada: dois brokers sem ASR que justifique |
| **(B') MQTT no plugin do RabbitMQ + consumidor AMQP no Core (ACL)** | **Sim** | **Sim** | **Sim** | **0** (plugin habilitado) | **Escolhida** |

## Decisão

- Tópico: `gameeducator/lab/{laboratorio}/dispositivo/{id}/evento`, que vira a routing key
  `gameeducator.lab.{lab}.dispositivo.{id}.evento`. Payload compacto:
  `{"id":"uuid","t":"P|I|C","a":alunoId,"atv":"...","ts":epoch}`.
- Usuário MQTT `dispositivo` com permissão de escrita **só** em `amq.topic` e com *topic permission*
  `^gameeducator\.lab\..*` (não publica em eventos internos). Anônimo desabilitado. Em produção: TLS (8883) e
  credencial por dispositivo.
- No Core, o módulo `iot`: `TradutorDeEventoIot` (camada anticorrupção, único lugar que conhece o formato do
  firmware) → `RegistroDeEventosIot` (idempotente, PK = `eventoId`) → evento `iot.evento-registrado` via outbox
  (ADR-B1-004), que encaminha ao ecossistema (fila `analytics.eventos`).
- Só o adaptador `ConsumidorIotRabbit` conhece AMQP, e nenhuma classe do Core conhece MQTT (verificado por ArchUnit).

## Consequências

- **Mais fácil:** um broker para operar e monitorar; o Core não sabe quantos nem quais dispositivos existem; uma
  troca de firmware afeta uma classe.
- **Mais difícil:** o dispositivo precisa de cliente MQTT e de buffer local, e a ordem global entre dispositivos
  não é garantida (usa-se `ts` do dispositivo). Também ficamos dependentes do plugin MQTT do RabbitMQ (limites de
  escala citados no gatilho).

## Evidência

`docs/arquitetura/evidencias/desafio-5/`: o simulador publica eventos com queda de conexão no meio, buffer local,
reenvio de duplicados e um payload inválido. O consumidor transforma os eventos e o Core registra cada evento
único exatamente uma vez.
