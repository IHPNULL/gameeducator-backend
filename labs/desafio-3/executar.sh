#!/usr/bin/env bash
# Desafio 3 - Spring Boot consumindo FastAPI; interromper/atrasar o servico Python e observar o tratamento da falha.
# Pre-requisito: docker compose -f labs/docker-compose.yml up -d --build   (a partir da raiz do gameeducator-backend)
set -euo pipefail
cd "$(dirname "$0")/../.."
source labs/comum/funcoes.sh
SAIDA=docs/arquitetura/evidencias/desafio-3/resultado.md
mkdir -p "$(dirname "$SAIDA")"
PERGUNTA='{"pergunta":"O que sao fracoes equivalentes?"}'

$COMPOSE up -d ia >/dev/null
esperar "$IA/saude"; esperar "$CORE/actuator/health"
curl -fs -X PUT "$IA/admin/atraso" -H 'Content-Type: application/json' -d '{"ms":0}' >/dev/null
sleep 16  # garante disjuntor fechado (espera de 15 s) caso o lab tenha rodado antes

linhas=()
registrar() { linhas+=("| $1 | \`$2\` | $3 | $4 ms | $5 |"); }

# 1) IA no ar
r=$(chamar GET "$CORE/api/cursos/$CURSO/recomendacoes"); registrar "1. IA no ar" "GET /recomendacoes" "${r% *}" "${r#* }" "origem=$(json "d['origem']"), modelo=$(json "d['modeloVersao']"), itens=$(json "len(d['itens'])")"
r=$(chamar POST "$CORE/api/cursos/$CURSO/assistente" "$PERGUNTA"); registrar "1. IA no ar" "POST /assistente" "${r% *}" "${r#* }" "provedor=$(json "d['provedor']"), fontes=$(json "[f['titulo'] for f in d['fontes']]")"

# 2) IA lenta (3 s > timeout de leitura de 2 s)
curl -fs -X PUT "$IA/admin/atraso" -H 'Content-Type: application/json' -d '{"ms":3000}' >/dev/null
r=$(chamar GET "$CORE/api/cursos/$CURSO/recomendacoes"); registrar "2. IA lenta (3 s)" "GET /recomendacoes" "${r% *}" "${r#* }" "origem=$(json "d['origem']") (timeout de 2 s do Core)"
curl -fs -X PUT "$IA/admin/atraso" -H 'Content-Type: application/json' -d '{"ms":0}' >/dev/null
r=$(chamar GET "$CORE/api/cursos/$CURSO/recomendacoes"); registrar "2. IA volta ao normal" "GET /recomendacoes" "${r% *}" "${r#* }" "origem=$(json "d['origem']")"

# 3) IA parada
$COMPOSE stop ia >/dev/null
for i in 1 2 3 4 5; do
  r=$(chamar GET "$CORE/api/cursos/$CURSO/recomendacoes"); registrar "3. IA parada (chamada $i)" "GET /recomendacoes" "${r% *}" "${r#* }" "origem=$(json "d['origem']"), itens=$(json "len(d['itens'])")"
done
r=$(chamar POST "$CORE/api/cursos/$CURSO/assistente" "$PERGUNTA"); registrar "3. IA parada" "POST /assistente" "${r% *}" "${r#* }" "$(json "d['mensagem']")"
r=$(chamar POST "$CORE/api/desafios/1/respostas" '{"indiceResposta":1}'); registrar "3. IA parada - fluxo essencial" "POST /desafios/1/respostas" "${r% *}" "${r#* }" "correta=$(json "d['correta']"), xpTotal=$(json "d['xpTotalNoCurso']")"
r=$(chamar GET "$CORE/api/cursos/$CURSO/ranking"); registrar "3. IA parada - fluxo essencial" "GET /ranking" "${r% *}" "${r#* }" "alunos no ranking=$(json "len(d)")"
METRICAS=$(curl -fs "$CORE/actuator/prometheus" | grep -E '^gameeducator_ia_(chamadas_total|disjuntor_aberto)' | sort)

# 4) Recuperacao (disjuntor meio-aberto -> fechado)
$COMPOSE start ia >/dev/null; esperar "$IA/saude"; sleep 16
r=$(chamar GET "$CORE/api/cursos/$CURSO/recomendacoes"); registrar "4. IA de volta (apos 15 s)" "GET /recomendacoes" "${r% *}" "${r#* }" "origem=$(json "d['origem']")"

{
  echo "# Desafio 3 - Core (Spring Boot) consumindo a IA (FastAPI) com falhas provocadas"
  echo
  echo "Gerado por \`labs/desafio-3/executar.sh\` em $(date '+%Y-%m-%d %H:%M'). Aluno Bruno (id $ALUNO), curso $CURSO."
  echo "Protecao no Core: timeout conexao 300 ms / leitura 2 s; disjuntor abre apos 3 falhas e testa de novo apos 15 s."
  echo
  echo "| Cenario | Chamada ao Core | HTTP | Tempo | Observado |"
  echo "|---|---|---|---|---|"
  printf '%s\n' "${linhas[@]}"
  echo
  echo "Metricas do Core logo apos o cenario 3 (\`/actuator/prometheus\`):"
  echo
  echo '```'
  echo "$METRICAS"
  echo '```'
} > "$SAIDA"
cat "$SAIDA"
