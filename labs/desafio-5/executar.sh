#!/usr/bin/env bash
# Desafio 5 - simulador publica eventos MQTT (com queda, buffer, duplicados e invalido); o Core consome e transforma.
# Pre-requisito: docker compose -f labs/docker-compose.yml up -d --build
set -euo pipefail
cd "$(dirname "$0")/../.."
source labs/comum/funcoes.sh
SAIDA=docs/arquitetura/evidencias/desafio-5/resultado.md
mkdir -p "$(dirname "$SAIDA")"
LAB="lab-$(date +%H%M%S)"
esperar "$CORE/actuator/health"

aceito0=$(metrica 'gameeducator_iot_eventos_total\{.*resultado="aceito"'); dup0=$(metrica 'gameeducator_iot_eventos_total\{.*resultado="duplicado"')
inv0=$(metrica 'gameeducator_iot_eventos_total\{.*resultado="invalido"'); dlq0=$(fila core.iot.eventos.dlq)

SIM=$($COMPOSE run --rm -T simulador-iot --host rabbitmq --laboratorio "$LAB" --dispositivo porta-01 \
      --eventos 20 --queda-apos 8 --eventos-offline 5 --duplicados 3 --invalidos 1 2>/dev/null)
sleep 5
curl -fs "$CORE/api/laboratorios/$LAB/eventos" > /tmp/lab-corpo.json
total=$(json "d['total']"); tipos=$(json "sorted({e['tipo'] for e in d['recentes']})")
gerados=$(echo "$SIM" | python3 -c "import sys,json; t=sys.stdin.read(); print(json.loads(t[t.index('{'):])['gerados'])")

# Seguranca (ASR-14): senha errada e topico fora da permissao do usuario "dispositivo"
SEG=$($COMPOSE run --rm -T --entrypoint python simulador-iot -c '
import paho.mqtt.client as mqtt, time
def tentar(senha, topico):
    c = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id="intruso-%d" % time.time_ns())
    c.username_pw_set("dispositivo", senha)
    res = {}
    c.on_connect = lambda cl, u, f, rc, p: res.setdefault("connect", str(rc))
    c.on_disconnect = lambda cl, u, f, rc, p: res.setdefault("disconnect", str(rc))
    try:
        c.connect("rabbitmq", 1883); c.loop_start(); time.sleep(1)
        if res.get("connect") == "Success":
            c.publish(topico, "{}", qos=1); time.sleep(1)
    finally:
        c.loop_stop()
    return res
print("senha errada:", tentar("errada", "gameeducator/lab/x/dispositivo/y/evento"))
print("topico interno gameeducator/eventos/forjado:", tentar("dispositivo", "gameeducator/eventos/forjado"))
' 2>/dev/null)

{
  echo "# Desafio 5 - eventos IoT via MQTT (RabbitMQ) com conectividade intermitente"
  echo
  echo "Gerado por \`labs/desafio-5/executar.sh\` em $(date '+%Y-%m-%d %H:%M'). Laboratorio \`$LAB\`, dispositivo \`porta-01\`."
  echo
  echo "## Saida do simulador"
  echo
  echo '```'
  echo "$SIM"
  echo '```'
  echo
  echo "## O que o Core registrou"
  echo
  echo "| Medida | Esperado | Observado |"
  echo "|---|---|---|"
  echo "| Eventos unicos gerados pelo dispositivo | - | $gerados |"
  echo "| Eventos registrados no Core (\`GET /api/laboratorios/$LAB/eventos\`) | $gerados (nenhuma perda) | $total |"
  echo "| Metrica aceito (delta) | $gerados | $(( $(metrica 'gameeducator_iot_eventos_total\{.*resultado="aceito"') - aceito0 )) |"
  echo "| Metrica duplicado (delta) | 3 (descartados) | $(( $(metrica 'gameeducator_iot_eventos_total\{.*resultado="duplicado"') - dup0 )) |"
  echo "| Metrica invalido (delta) | 1 | $(( $(metrica 'gameeducator_iot_eventos_total\{.*resultado="invalido"') - inv0 )) |"
  echo "| Mensagens na DLQ \`core.iot.eventos.dlq\` | +1 | $dlq0 -> $(fila core.iot.eventos.dlq) |"
  echo "| Tipos canonicos traduzidos pela ACL | PRESENCA/INICIO_ATIVIDADE/CONCLUSAO_ATIVIDADE | $tipos |"
  echo "| Encaminhados ao ecossistema (fila \`analytics.eventos\`) | >= $gerados | $(fila analytics.eventos) mensagens prontas |"
  echo
  echo "## Seguranca da integracao"
  echo
  echo '```'
  echo "$SEG"
  echo '```'
} > "$SAIDA"
cat "$SAIDA"
