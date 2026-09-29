package org.facens.grupo_6_gameeducator.service.dto;

import org.facens.grupo_6_gameeducator.domain.Usuario;

/**
 * Desempenho agregado de um aluno em um curso: XP, tentativas e acertos.
 * Fonte para o painel do professor acompanhar a turma.
 */
public record DesempenhoAluno(Usuario aluno, int xpTotal, int tentativas, int acertos) {
}
