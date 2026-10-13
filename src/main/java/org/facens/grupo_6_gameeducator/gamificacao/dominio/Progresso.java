package org.facens.grupo_6_gameeducator.gamificacao.dominio;

/**
 * XP acumulado por um aluno em um curso. Objeto de valor imutavel: creditar XP gera um novo
 * progresso, e o antigo continua disponivel para a regra de medalhas comparar antes/depois.
 */
public record Progresso(Long alunoId, Long cursoId, int xpTotal) {

    public Progresso {
        if (xpTotal < 0) {
            throw new IllegalArgumentException("XP total nao pode ser negativo");
        }
    }

    public static Progresso inicial(Long alunoId, Long cursoId) {
        return new Progresso(alunoId, cursoId, 0);
    }

    public Progresso creditar(int xp) {
        if (xp < 0) {
            throw new IllegalArgumentException("XP creditado nao pode ser negativo");
        }
        return new Progresso(alunoId, cursoId, xpTotal + xp);
    }
}
