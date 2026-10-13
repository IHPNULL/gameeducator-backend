#!/usr/bin/env bash
# Desafio 2 - roda os testes que provam a troca de adaptador e resume o resultado.
# Uso (raiz do gameeducator-backend): labs/desafio-2/relatorio-adaptadores.sh
set -euo pipefail
cd "$(dirname "$0")/../.."
SAIDA=docs/arquitetura/evidencias/desafio-2/troca-de-adaptador.md
CLASSES="RegrasDeGamificacaoTest,GamificacaoServiceTest,RepositoriosEmMemoriaTest,RepositoriosJpaTest,TrocaDeAdaptadorNoContextoTest,AlunoGanhaMedalhaTest,ArquiteturaModularTest"
./mvnw -q -B test -Dtest="$CLASSES" -Djacoco.skip=true > /tmp/d2.log 2>&1
{
  echo "# Desafio 2 - mesma regra, adaptadores diferentes"
  echo
  echo "Gerado por \`labs/desafio-2/relatorio-adaptadores.sh\` em $(date '+%Y-%m-%d %H:%M')."
  echo
  echo "| Classe de teste | O que prova | Adaptador | Spring? | Testes | Falhas | Tempo (s) |"
  echo "|---|---|---|---|---|---|---|"
  linha() { # classe, prova, adaptador, spring
    f=$(ls target/surefire-reports/*."$1".txt)
    read -r t fa er tempo < <(grep "Tests run" "$f" | sed -E 's/.*Tests run: ([0-9]+), Failures: ([0-9]+), Errors: ([0-9]+).*elapsed: ([0-9.]+).*/\1 \2 \3 \4/')
    echo "| \`$1\` | $2 | $3 | $4 | $t | $((fa + er)) | $tempo |"
  }
  linha RegrasDeGamificacaoTest "regras de XP/medalhas isoladas" "nenhum" "nao" 
  linha GamificacaoServiceTest "caso de uso completo" "memoria" "nao"
  linha RepositoriosEmMemoriaTest "contrato das portas" "memoria" "nao"
  linha RepositoriosJpaTest "MESMO contrato das portas" "JPA + H2" "sim"
  linha TrocaDeAdaptadorNoContextoTest "app sobe com outro adaptador so por propriedade" "memoria" "sim"
  linha AlunoGanhaMedalhaTest "cenarios BDD da AC1 inalterados" "JPA + H2" "sim"
  linha ArquiteturaModularTest "dominio sem Spring/JPA; aplicacao sem adaptador" "-" "nao"
} > "$SAIDA"
cat "$SAIDA"
