#!/usr/bin/env bash
# Desafio 4 - interacao sincrona x assincrona com o consumidor de notificacao indisponivel; broker indisponivel.
# Pre-requisito: docker compose -f labs/docker-compose.yml up -d --build
set -euo pipefail
cd "$(dirname "$0")/../.."
source labs/comum/funcoes.sh
SAIDA=docs/arquitetura/evidencias/desafio-4/resultado.md
mkdir -p "$(dirname "$SAIDA")"

subir_core() { # modo
  EVENTOS_MODO=$1 $COMPOSE up -d backend >/dev/null
  sleep 5; esperar "$CORE/actuator/health"
}
notificacoes() { curl -fs "$NOTIF/notificacoes" | python3 -c "import json,sys; print(json.load(sys.stdin)['totalUnicas'])"; }
tentativas() { curl -fs "$CORE/api/desafios/$1/tentativas" -H "X-Usuario-Id: $ALUNO" | python3 -c "import json,sys; print(len(json.load(sys.stdin)))"; }

linhas=()
registrar() { linhas+=("| $1 | $2 | $3 | $4 |"); }

# ---------- A) modo SINCRONO (alternativa descartada), consumidor fora ----------
subir_core sincrono
$COMPOSE stop notificacao >/dev/null
read -r M D <<<"$(nova_missao sincrono)"
r=$(chamar POST "$CORE/api/desafios/$D/respostas" '{"indiceResposta":1}')
registrar "A. Sincrono (REST), notificacao FORA" "HTTP ${r% *} em ${r#* } ms - $(json "d.get('mensagem', d)")" "tentativas gravadas: $(tentativas "$D")" "a conclusao FALHOU e foi desfeita (rollback)"
$COMPOSE start notificacao >/dev/null; esperar "$NOTIF/saude"
antes=$(notificacoes)
r=$(chamar POST "$CORE/api/desafios/$D/respostas" '{"indiceResposta":1}')
registrar "A. Sincrono (REST), notificacao NO AR" "HTTP ${r% *} em ${r#* } ms - missaoConcluida=$(json "d['missaoConcluida']")" "tentativas: $(tentativas "$D"); notificacoes: $antes -> $(notificacoes)" "funciona, mas so com o consumidor no ar"

# ---------- B) modo ASSINCRONO (decisao), consumidor fora ----------
subir_core assincrono
$COMPOSE stop notificacao >/dev/null
read -r M D <<<"$(nova_missao assincrono)"
r=$(chamar POST "$CORE/api/desafios/$D/respostas" '{"indiceResposta":1}')
sleep 5
registrar "B. Assincrono (outbox+RabbitMQ), notificacao FORA" "HTTP ${r% *} em ${r#* } ms - missaoConcluida=$(json "d['missaoConcluida']")" "tentativas: $(tentativas "$D"); outbox pendentes: $(metrica gameeducator_outbox_pendentes); fila notificacao.eventos: $(fila notificacao.eventos)" "conclusao OK; evento retido na fila"
$COMPOSE start notificacao >/dev/null; esperar "$NOTIF/saude"; sleep 5
registrar "B. ... consumidor volta" "-" "fila notificacao.eventos: $(fila notificacao.eventos); notificacoes unicas: $(notificacoes)" "fila drenou, nada perdido"

# ---------- C) modo ASSINCRONO, BROKER fora ----------
$COMPOSE stop rabbitmq >/dev/null
read -r M D <<<"$(nova_missao broker-fora)"
r=$(chamar POST "$CORE/api/desafios/$D/respostas" '{"indiceResposta":1}')
sleep 6
registrar "C. Assincrono, RabbitMQ FORA" "HTTP ${r% *} em ${r#* } ms - missaoConcluida=$(json "d['missaoConcluida']")" "outbox pendentes: $(metrica gameeducator_outbox_pendentes); falhas de publicacao: $(metrica 'gameeducator_eventos_publicados_total\{.*resultado="falha"')" "conclusao OK; evento seguro no banco"
$COMPOSE start rabbitmq >/dev/null; esperar "$RABBIT_API/overview" 60 || true; sleep 20
registrar "C. ... broker volta" "-" "outbox pendentes: $(metrica gameeducator_outbox_pendentes); fila notificacao.eventos: $(fila notificacao.eventos); notificacoes unicas: $(notificacoes)" "relay reenviou; consumidor recebeu"

{
  echo "# Desafio 4 - sincrono x assincrono com consumidor e broker indisponiveis"
  echo
  echo "Gerado por \`labs/desafio-4/executar.sh\` em $(date '+%Y-%m-%d %H:%M')."
  echo "Cada cenario cria uma missao nova de 1 desafio (professor Ana) e o aluno Bruno a conclui (POST /api/desafios/{id}/respostas)."
  echo
  echo "| Cenario | Resposta ao aluno | Estado observado | Leitura |"
  echo "|---|---|---|---|"
  printf '%s\n' "${linhas[@]}"
  echo
  echo "Ultimas notificacoes recebidas pelo consumidor (canal http = modo sincrono; amqp = modo assincrono):"
  echo
  echo '```json'
  curl -fs "$NOTIF/notificacoes" | python3 -m json.tool | head -60
  echo '```'
} > "$SAIDA"
cat "$SAIDA"
