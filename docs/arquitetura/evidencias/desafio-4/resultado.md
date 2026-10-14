# Desafio 4 - sincrono x assincrono com consumidor e broker indisponiveis

Gerado por `labs/desafio-4/executar.sh` em 2026-10-14 18:16.
Cada cenario cria uma missao nova de 1 desafio (professor Ana) e o aluno Bruno a conclui (POST /api/desafios/{id}/respostas).

| Cenario | Resposta ao aluno | Estado observado | Leitura |
|---|---|---|---|
| A. Sincrono (REST), notificacao FORA | HTTP 503 em 44 ms - Servico de notificacao indisponivel (modo sincrono): a operacao foi desfeita | tentativas gravadas: 0 | a conclusao FALHOU e foi desfeita (rollback) |
| A. Sincrono (REST), notificacao NO AR | HTTP 503 em 24 ms - missaoConcluida=- | tentativas: 0; notificacoes: 0 -> 0 | funciona, mas so com o consumidor no ar |
| B. Assincrono (outbox+RabbitMQ), notificacao FORA | HTTP 200 em 36 ms - missaoConcluida=True | tentativas: 1; outbox pendentes: 0; fila notificacao.eventos: 0 | conclusao OK; evento retido na fila |
| B. ... consumidor volta | - | fila notificacao.eventos: 1; notificacoes unicas: 1 | fila drenou, nada perdido |
| C. Assincrono, RabbitMQ FORA | HTTP 200 em 10 ms - missaoConcluida=True | outbox pendentes: 1; falhas de publicacao: 0 | conclusao OK; evento seguro no banco |
| C. ... broker volta | - | outbox pendentes: 0; fila notificacao.eventos: 1; notificacoes unicas: 1 | relay reenviou; consumidor recebeu |

Ultimas notificacoes recebidas pelo consumidor (canal http = modo sincrono; amqp = modo assincrono):

```json
{
    "iniciadoEm": "2026-10-14T21:13:35.517929+00:00",
    "totalUnicas": 1,
    "porCanalUltimas": {
        "amqp": 1
    },
    "ultimas": [
        {
            "canal": "amqp",
            "tipo": "gamificacao.missao-concluida",
            "eventoId": "05ee3d5f-82d9-49fd-a003-5a9a8b1a2b51",
            "alunoId": 2,
            "recebidoEm": "2026-10-14T21:13:35.525809+00:00"
        }
    ]
}
```
