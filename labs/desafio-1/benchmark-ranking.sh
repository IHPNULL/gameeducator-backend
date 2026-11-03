#!/usr/bin/env bash
# Desafio 1 - mede a consulta de ranking (mesma do ProgressoAlunoRepository) em PostgreSQL 16
# com 100k usuarios, SEM e COM o indice ix_progresso_curso_xp, ranking completo e top-50.
# Uso (raiz do gameeducator-backend; precisa de Docker): labs/desafio-1/benchmark-ranking.sh
set -euo pipefail
cd "$(dirname "$0")"
SAIDA=../../docs/arquitetura/evidencias/desafio-1/benchmark-ranking.md
NOME=ge-bench-ranking
docker rm -f $NOME >/dev/null 2>&1 || true
docker run -d --name $NOME -e POSTGRES_PASSWORD=bench postgres:16-alpine >/dev/null
trap 'docker rm -f $NOME >/dev/null 2>&1 || true' EXIT
until docker exec $NOME pg_isready -U postgres >/dev/null 2>&1; do sleep 1; done
sleep 2
docker exec -i $NOME psql -q -v ON_ERROR_STOP=1 -U postgres < benchmark-ranking.sql

COMPLETO="SELECT p.aluno_id, u.nome, p.xp_total FROM progresso_aluno p JOIN usuario u ON u.id = p.aluno_id WHERE p.curso_id = 1 ORDER BY p.xp_total DESC"
TOP50="$COMPLETO LIMIT 50"
TIPICO="SELECT p.aluno_id, u.nome, p.xp_total FROM progresso_aluno p JOIN usuario u ON u.id = p.aluno_id WHERE p.curso_id = 77 ORDER BY p.xp_total DESC"

mediana() { # executa 7x e devolve a mediana do "Execution Time" do EXPLAIN ANALYZE (ms)
  for i in $(seq 1 7); do
    docker exec $NOME psql -U postgres -At -c "EXPLAIN (ANALYZE, FORMAT JSON) $1" \
      | python3 -c 'import json,sys; print(json.load(sys.stdin)[0]["Execution Time"])'
  done | sort -n | sed -n 4p
}
plano() { docker exec $NOME psql -U postgres -At -c "EXPLAIN $1" | head -3 | sed 's/^/    /'; }

{
  echo "# Desafio 1 - benchmark do ranking (PostgreSQL 16, 100.000 usuarios)"
  echo
  echo "Gerado por \`labs/desafio-1/benchmark-ranking.sh\` em $(date '+%Y-%m-%d %H:%M'). Mediana de 7 execucoes (EXPLAIN ANALYZE, ms)."
  echo
  echo "Massa: $(docker exec $NOME psql -U postgres -At -F' / ' -c 'SELECT count(*) || '"' linhas de progresso'"', count(DISTINCT curso_id) || '"' cursos'"' FROM progresso_aluno')."
  echo "Curso 1 = pior caso (20.000 alunos). Curso 77 = curso tipico (~1.500 alunos)."
  echo
  echo "| Consulta | Sem indice (ms) | Com ix_progresso_curso_xp (ms) |"
  echo "|---|---|---|"
  A1=$(mediana "$TIPICO"); A2=$(mediana "$COMPLETO"); A3=$(mediana "$TOP50")
  P_SEM=$(plano "$TOP50")
  docker exec $NOME psql -q -U postgres -c "CREATE INDEX ix_progresso_curso_xp ON progresso_aluno (curso_id, xp_total DESC); ANALYZE progresso_aluno;"
  B1=$(mediana "$TIPICO"); B2=$(mediana "$COMPLETO"); B3=$(mediana "$TOP50")
  P_COM=$(plano "$TOP50")
  echo "| Ranking completo, curso tipico | $A1 | $B1 |"
  echo "| Ranking completo, curso de 20k alunos (consulta atual da API) | $A2 | $B2 |"
  echo "| Top-50, curso de 20k alunos | $A3 | $B3 |"
  echo
  echo "Plano do top-50 sem indice:"; echo; echo "$P_SEM"; echo
  echo "Plano do top-50 com indice:"; echo; echo "$P_COM"
} > "$SAIDA"
cat "$SAIDA"
