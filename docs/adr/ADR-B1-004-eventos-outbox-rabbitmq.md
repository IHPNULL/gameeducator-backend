# ADR-B1-004: Síncrono para o que é imediato; eventos via Transactional Outbox + RabbitMQ para o que é eventual

**Status:** Aceito · **Desafio:** 4 · **Data:** 2026-10

## Contexto

Ao concluir uma atividade, o sistema registra a conclusão, calcula recompensas (XP e medalhas), atualiza o
progresso e pode disparar notificação e analytics. **Uma falha de notificação não pode impedir a conclusão.**

No GameEducator, "concluir a atividade" é **concluir a missão**: a resposta que acerta o último desafio ainda
não acertado. A detecção foi implementada em `JogoService.responder`.

## ASRs e RPCs

| ASR | Como altera a arquitetura |
|---|---|
| **ASR-09 Tolerância a falhas de consumidores externos** (FURPS+ *Reliability*) | Muda a **comunicação**: notificação e analytics saem do caminho síncrono da requisição. |
| **ASR-10 Recuperabilidade (nenhum evento perdido)** (FURPS+ *Reliability*) | Muda **dados e operação**: o evento precisa ser durável antes da entrega (outbox no banco, fila durável, DLQ). |
| **ASR-11 Consistência do resultado para o aluno** (FURPS+ *Functionality*/integridade) | Muda **dados**: XP, medalha e conclusão são atômicos e imediatos; notificação e analytics podem ser eventuais. |

Não selecionado como ASR: *latência*, porque a resposta já é rápida e tirar a notificação do caminho só ajuda.

RPCs: operações essenciais concluem mesmo com consumidor externo indisponível (RPC-07); adotar broker tem
custo operacional (RPC-08); equipe de 6 (RPC-02).

## Respostas do Tech Lead

| O quê | Imediato ou eventual | Mecanismo |
|---|---|---|
| Registrar tentativa, creditar XP, conceder medalha, detectar conclusão | **Imediato e atômico** | Transação local no Core (síncrono) |
| Notificar o aluno (medalha, missão concluída) | **Eventual** (segundos) | Evento → RabbitMQ → serviço de notificação |
| Analytics de aprendizagem | **Eventual** (minutos) | Mesmo evento, fila própria (*fan-out*) |

- **Que ASR justifica mensageria?** ASR-09 junto com ASR-10. Só o ASR-09 poderia ser resolvido com `@Async` em
  memória, mas aí um restart perde o evento, o que viola o ASR-10. Só o ASR-10 seria resolvido com outbox + retry
  HTTP, mas aí cada novo consumidor exige mudar o Core, e não há *fan-out*.
- **O RabbitMQ resolve um problema real do fluxo?** Sim. Oferece fila durável por consumidor (notificação e analytics
  independentes), ack/nack com DLQ para mensagem inválida, roteamento por tipo de evento e retenção enquanto o
  consumidor está fora. E no Desafio 5 o mesmo broker atende MQTT (plugin), então o custo operacional é pago uma vez só.
- **E o Kafka?** Resolveria *replay* de histórico e alto throughput para analytics em *stream*. Hoje o volume é de
  dezenas de eventos por segundo, não há requisito de replay e a equipe teria que operar um cluster (KRaft,
  partições, retenção). **Gatilho:** analytics exigir reprocessar o histórico ou o volume chegar a milhares de eventos por segundo.

## Alternativas

| Alternativa | ASR-09 | ASR-10 | ASR-11 | Custo | Veredito |
|---|---|---|---|---|---|
| REST síncrono para a notificação dentro da requisição | **Falha da notificação = falha da conclusão** (provado no lab) | Sem retry | Atômico, mas pelo motivo errado | Zero | Rejeitada (viola RPC-07) |
| `@Async` / evento em memória (Spring events) | Sim | **Não**: restart ou queda perde o evento | Sim | Zero | Rejeitada |
| Publicar direto no broker dentro da transação | Sim | **Dual write**: broker fora derruba a requisição, ou o banco faz rollback depois de publicar | Inconsistente | Médio | Rejeitada |
| **Outbox + relay + RabbitMQ** | **Sim** | **Sim**: evento no banco na mesma transação, publisher confirm, fila durável, DLQ | **Sim** | Médio (broker + relay) | **Escolhida** |
| Outbox + Kafka | Sim | Sim (+ replay) | Sim | Alto (cluster, partições) | Adiada (gatilho acima) |

## Decisão

1. `JogoService.responder` faz tudo o que é imediato e emite `MissaoConcluida` e `MedalhaConquistada` pela porta
   `EmissorDeEventos`.
2. O adaptador padrão `EmissorViaOutbox` grava o evento na tabela `outbox_evento` **na mesma transação**
   (`Propagation.MANDATORY`).
3. O `RelayDoOutbox` publica os pendentes a cada 2 s, em ordem, no exchange topic `gameeducator.eventos`
   (routing key = tipo), com mensagem persistente e **publisher confirm**. Se o broker não confirmar, o evento
   fica pendente e o lote para.
4. A topologia é versionada em `labs/infra/rabbitmq/definitions.json` (filas *quorum* `notificacao.eventos` e
   `analytics.eventos`, DLX `gameeducator.dlx` e DLQs).
5. A entrega é **pelo menos uma vez**: o `eventoId` é a chave de idempotência dos consumidores.
6. O modo `app.eventos.modo=sincrono` (REST direto) existe **só** para o lab comparativo.

## Consequências

- **Mais fácil:** a conclusão nunca depende de terceiros; novos consumidores entram com um *binding* na topologia,
  sem mudar o Core; o atraso é visível (`gameeducator_outbox_pendentes`, profundidade da fila).
- **Mais difícil:** consistência eventual para a notificação; consumidores precisam ser idempotentes; o relay é
  mais um componente (com várias réplicas do Core, usar `SELECT ... FOR UPDATE SKIP LOCKED`, que é pendência
  registrada); e há um broker para operar.
- **Trade-off aceito:** latência de alguns segundos na notificação em troca de a conclusão nunca falhar por causa dela.

## Evidência

`docs/arquitetura/evidencias/desafio-4/`: a mesma conclusão de missão com o consumidor de notificação fora, em modo
síncrono (falha e rollback) e em modo assíncrono (sucesso, fila retém e entrega quando volta), e também com o
**broker** fora (outbox acumula e esvazia quando ele volta).
