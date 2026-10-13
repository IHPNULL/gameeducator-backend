#!/usr/bin/env bash
# Desafio 6 - provoca condicoes observaveis e registra o efeito nas metricas/alertas do Prometheus (e Grafana).
# Pre-requisito: docker compose -f labs/docker-compose.yml up -d --build
set -euo pipefail
cd "$(dirname "$0")/../.."
source labs/comum/funcoes.sh
DIR=docs/arquitetura/evidencias/desafio-6
SAIDA=$DIR/resultado.md
mkdir -p "$DIR"
esperar "$CORE/actuator/health"; esperar "$PROM/-/ready"

trafego() { # segundos
  local fim=$(( $(date +%s) + $1 ))
  while [ "$(date +%s)" -lt "$fim" ]; do
    curl -s -o /dev/null "$CORE/api/cursos/$CURSO/ranking"
    curl -s -o /dev/null -H "X-Usuario-Id: $ALUNO" "$CORE/api/cursos/$CURSO/recomendacoes"
    curl -s -o /dev/null -X POST -H "X-Usuario-Id: $ALUNO" -H 'Content-Type: application/json' -d '{"indiceResposta":1}' "$CORE/api/desafios/1/respostas"
    sleep 0.3
  done
}
alertas() { curl -fs "$PROM/api/v1/alerts" | python3 -c "
import json,sys
a=json.load(sys.stdin)['data']['alerts']
print(', '.join(f\"{x['labels']['alertname']}={x['state']}\" for x in a) or 'nenhum')"; }
alvos() { curl -fs "$PROM/api/v1/targets" | python3 -c "
import json,sys
print(', '.join(f\"{t['labels']['job']}={t['health']}\" for t in json.load(sys.stdin)['data']['activeTargets']))"; }

P95_REC='histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{uri="/api/cursos/{cursoId}/recomendacoes"}[1m])))'
P95_RANK='histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{uri="/api/cursos/{cursoId}/ranking"}[1m])))'
IA='sum by (resultado) (increase(gameeducator_ia_chamadas_total[1m]))'
ERROS='sum(rate(http_server_requests_seconds_count{uri=~"/api/.*",status=~"5.."}[1m]))'

linhas=()
foto() { linhas+=("| $1 | $(promql "$P95_REC") s | $(promql "$P95_RANK") s | $(promql "$IA") | $(promql 'max(gameeducator_ia_disjuntor_aberto)') | $(promql 'max(gameeducator_outbox_pendentes)') | $(promql 'sum(rabbitmq_queue_messages_ready{queue="notificacao.eventos"})') | $(promql "$ERROS") | $(alertas) |"); }

echo "linha de base (60 s de trafego)..."; trafego 60; foto "0. Linha de base"
echo "IA parada (75 s)..."; $COMPOSE stop ia >/dev/null; trafego 75; foto "1. IA parada"
$COMPOSE start ia >/dev/null; esperar "$IA/saude"; trafego 45; foto "1'. IA de volta"
echo "consumidor de notificacao parado..."; $COMPOSE stop notificacao >/dev/null
for i in 1 2 3 4 5 6 7 8 9 10 11 12; do read -r M D <<<"$(nova_missao obs)"; chamar POST "$CORE/api/desafios/$D/respostas" '{"indiceResposta":1}' >/dev/null; done
trafego 70; foto "2. Notificacao parada (12 conclusoes)"
$COMPOSE start notificacao >/dev/null; esperar "$NOTIF/saude"; trafego 30; foto "2'. Notificacao de volta"

# Grafana (opcional): screenshot do dashboard com Chrome headless, se disponivel
CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
PNG=""
if [ -x "$CHROME" ]; then
  "$CHROME" --headless=new --disable-gpu --hide-scrollbars --window-size=1600,2600 --virtual-time-budget=20000 \
    --screenshot="$PWD/$DIR/grafana-dashboard.png" \
    "http://localhost:3000/d/gameeducator-b1/?orgId=1&from=now-12m&to=now&kiosk" >/dev/null 2>&1 && PNG="ok"
fi

{
  echo "# Desafio 6 - condicoes provocadas e efeito nas metricas (Prometheus)"
  echo
  echo "Gerado por \`labs/desafio-6/executar.sh\` em $(date '+%Y-%m-%d %H:%M'). Trafego sintetico: ranking, recomendacoes e respostas a cada 0,3 s."
  echo "Alvos do Prometheus no inicio: $(alvos)."
  echo
  echo "| Momento | p95 /recomendacoes | p95 /ranking | Chamadas Core->IA (1 min) | Disjuntor aberto | Outbox pendentes | Fila notificacao.eventos | Erros 5xx/s | Alertas |"
  echo "|---|---|---|---|---|---|---|---|---|"
  printf '%s\n' "${linhas[@]}"
  echo
  echo "Consultas PromQL usadas:"
  echo
  echo '```'
  echo "p95 recomendacoes : $P95_REC"
  echo "p95 ranking       : $P95_RANK"
  echo "chamadas IA       : $IA"
  echo "erros 5xx         : $ERROS"
  echo '```'
  [ -n "$PNG" ] && { echo; echo "![Dashboard Grafana](grafana-dashboard.png)"; }
} > "$SAIDA"
cat "$SAIDA"
