package org.facens.grupo_6_gameeducator.gamificacao;

import java.util.List;

/**
 * Porta de entrada do modulo de gamificacao. Outros modulos do Core (jogo, web, eventos)
 * so conhecem esta interface e os records deste pacote: nunca as entidades, repositorios
 * ou regras internas do modulo.
 */
public interface GamificacaoApi {

    /** Aplica as regras de XP e de medalhas a uma resposta ja validada pelo modulo de jogo. */
    ResultadoPontuacao pontuarResposta(RespostaPontuavel resposta);

    /** XP acumulado do aluno no curso. Zero enquanto ele nao pontuou. */
    int xpNoCurso(Long alunoId, Long cursoId);

    /** Ranking de XP da turma, do maior para o menor, com a posicao de cada aluno. */
    List<PosicaoNoRanking> ranking(Long cursoId);

    /** Medalhas do aluno no curso, do menor para o maior marco. */
    List<MedalhaDoAluno> medalhasDoAluno(Long alunoId, Long cursoId);
}
