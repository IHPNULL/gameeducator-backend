#!/usr/bin/env bash
# Funcoes compartilhadas pelos scripts dos desafios 3 a 6 (stack labs/docker-compose.yml no ar).
COMPOSE="docker compose -f labs/docker-compose.yml"
CORE=http://localhost:8080
IA=http://localhost:8000
RABBIT_API=http://localhost:15672/api
RABBIT_AUTH=gameeducator:gameeducator
PROM=http://localhost:9090
NOTIF=http://localhost:8085
PROFESSOR=1   # Ana  (dados de exemplo do perfil dev)
ALUNO=2       # Bruno
CURSO=1

esperar() { # url [tentativas]
  for _ in $(seq 1 "${2:-90}"); do curl -fs -o /dev/null "$1" && return 0; sleep 2; done
  echo "ERRO: $1 nao respondeu" >&2; return 1
}

# chamar METODO URL [CORPO] -> imprime "status ms" e deixa o corpo em /tmp/lab-corpo.json
chamar() {
  local metodo=$1 url=$2 corpo=${3:-}
  local args=(-s -o /tmp/lab-corpo.json -w '%{http_code} %{time_total}' -X "$metodo" -H "X-Usuario-Id: $ALUNO")
  [ -n "$corpo" ] && args+=(-H 'Content-Type: application/json' -d "$corpo")
  curl "${args[@]}" "$url" | awk '{printf "%s %.0f", $1, $2 * 1000}'
}

json() { # expressao python sobre o corpo da ultima chamada (variavel d)
  python3 -c "import json,sys; d=json.load(open('/tmp/lab-corpo.json')); print($1)" 2>/dev/null || echo "-"
}

nova_missao() { # cria missao de 1 desafio (indice correto 1) como professor; imprime "missaoId desafioId"
  curl -fs -X POST "$CORE/api/cursos/$CURSO/missoes" -H "X-Usuario-Id: $PROFESSOR" -H 'Content-Type: application/json' \
    -d "{\"titulo\":\"Lab $1 $(date +%H%M%S)\",\"descricao\":\"missao do lab\",\"desafios\":[{\"enunciado\":\"Quanto e 2+2?\",\"alternativas\":[\"3\",\"4\"],\"indiceRespostaCorreta\":1,\"xp\":5}]}" \
    | python3 -c "import json,sys; m=json.load(sys.stdin); print(m['id'], m['desafios'][0]['id'])"
}

fila() { # mensagens prontas na fila
  curl -fs -u "$RABBIT_AUTH" "$RABBIT_API/queues/%2F/$1" | python3 -c "import json,sys; print(json.load(sys.stdin).get('messages_ready', 0))" 2>/dev/null || echo "?"
}

metrica() { # valor de uma serie do /actuator/prometheus do Core (soma das linhas que casam)
  curl -fs "$CORE/actuator/prometheus" | grep -E "^$1" | awk '{s+=$NF} END {print s+0}'
}

promql() { # consulta instantanea no Prometheus -> "rotulos=valor; ..."
  curl -fs --get "$PROM/api/v1/query" --data-urlencode "query=$1" | python3 -c "
import json,sys
r=json.load(sys.stdin)['data']['result']
print('; '.join((','.join(f'{k}={v}' for k,v in x['metric'].items() if k not in ('__name__','instance','job','application')) or 'valor') + '=' + str(round(float(x['value'][1]),4)) for x in r) or 'sem dados')"
}
