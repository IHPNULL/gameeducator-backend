package org.facens.grupo_6_gameeducator.gamificacao;

/**
 * Fato que o modulo de jogo entrega a gamificacao: o aluno respondeu um desafio do curso.
 *
 * @param xpDoDesafio XP que o desafio vale no primeiro acerto
 * @param jaHaviaAcertado se o aluno ja tinha acertado este desafio antes (XP e creditado uma unica vez)
 */
public record RespostaPontuavel(Long alunoId, Long cursoId, int xpDoDesafio, boolean correta, boolean jaHaviaAcertado) {
}
