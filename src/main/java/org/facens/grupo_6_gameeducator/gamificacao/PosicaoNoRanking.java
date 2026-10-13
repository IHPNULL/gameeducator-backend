package org.facens.grupo_6_gameeducator.gamificacao;

/** Linha do ranking da turma. */
public record PosicaoNoRanking(int posicao, Long alunoId, String nomeAluno, int xpTotal) {
}
