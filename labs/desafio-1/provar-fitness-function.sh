#!/usr/bin/env bash
# Desafio 1/2 - prova de que a fronteira do modulo e verificada (e nao apenas desenhada):
# cria temporariamente uma classe na camada web que usa a entidade JPA interna da gamificacao,
# roda so o ArquiteturaModularTest (deve FALHAR) e remove a classe (deve voltar a PASSAR).
# Uso (raiz do gameeducator-backend): labs/desafio-1/provar-fitness-function.sh
set -uo pipefail
cd "$(dirname "$0")/../.."
SAIDA=docs/arquitetura/evidencias/desafio-1/fitness-function.txt
VIOLADORA=src/main/java/org/facens/grupo_6_gameeducator/web/AtalhoProibido.java

cat > "$VIOLADORA" <<'JAVA'
package org.facens.grupo_6_gameeducator.web;

import org.facens.grupo_6_gameeducator.gamificacao.adaptador.jpa.ProgressoAluno;

/** Classe temporaria do lab: web acessando a entidade interna da gamificacao (proibido por ADR-B1-001). */
class AtalhoProibido {
    int xp(ProgressoAluno progresso) {
        return progresso.getXpTotal();
    }
}
JAVA

{
  echo "== 1) com a classe violadora web.AtalhoProibido -> gamificacao.adaptador.jpa.ProgressoAluno"
  ./mvnw -q -B test -Dtest=ArquiteturaModularTest -Djacoco.skip=true 2>&1 | grep -E "Rule '|was violated|Class <|Tests run|BUILD|FAIL" | head -20
  echo "exit code: ${PIPESTATUS[0]}"
} > "$SAIDA"
rm -f "$VIOLADORA"
{
  echo
  echo "== 2) classe removida"
  ./mvnw -q -B test -Dtest=ArquiteturaModularTest -Djacoco.skip=true > /tmp/fitness-ok.log 2>&1
  echo "exit code: $?"
  grep -h "Tests run" target/surefire-reports/*ArquiteturaModularTest.txt
} >> "$SAIDA"
cat "$SAIDA"
