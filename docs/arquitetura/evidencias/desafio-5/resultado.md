# Desafio 5 - eventos IoT via MQTT (RabbitMQ) com conectividade intermitente

Gerado por `labs/desafio-5/executar.sh` em 2026-10-14 18:22. Laboratorio `lab-182215`, dispositivo `porta-01`.

## Saida do simulador

```
-- conexao perdida apos 8 eventos; gerando offline (buffer local)
-- reconectado; descarregando 5 eventos do buffer
{
  "gerados": 20,
  "publicadosOnline": 15,
  "bufferizadosOffline": 5,
  "descarregadosAposReconexao": 5,
  "duplicadosEnviados": 3,
  "invalidosEnviados": 1,
  "eventosUnicosEsperadosNoCore": 20,
  "topico": "gameeducator/lab/lab-182215/dispositivo/porta-01/evento",
  "payloadExemplo": "{\"id\":\"5523b89c-913d-44ae-8f36-380c38bab76b\",\"t\":\"P\",\"a\":1000,\"atv\":\"redes-1\",\"ts\":1792012936}",
  "bytesPorEventoMqtt": 155,
  "bytesPorEventoRestMinimo": 263
}
```

## O que o Core registrou

| Medida | Esperado | Observado |
|---|---|---|
| Eventos unicos gerados pelo dispositivo | - | 20 |
| Eventos registrados no Core (`GET /api/laboratorios/lab-182215/eventos`) | 20 (nenhuma perda) | 20 |
| Metrica aceito (delta) | 20 | 20 |
| Metrica duplicado (delta) | 3 (descartados) | 3 |
| Metrica invalido (delta) | 1 | 1 |
| Mensagens na DLQ `core.iot.eventos.dlq` | +1 | 2 -> 3 |
| Tipos canonicos traduzidos pela ACL | PRESENCA/INICIO_ATIVIDADE/CONCLUSAO_ATIVIDADE | ['CONCLUSAO_ATIVIDADE', 'INICIO_ATIVIDADE', 'PRESENCA'] |
| Encaminhados ao ecossistema (fila `analytics.eventos`) | >= 20 | 62 mensagens prontas |

## Seguranca da integracao

```
senha errada: {}
topico interno gameeducator/eventos/forjado: {'connect': 'Success', 'disconnect': 'Unspecified error'}
```
