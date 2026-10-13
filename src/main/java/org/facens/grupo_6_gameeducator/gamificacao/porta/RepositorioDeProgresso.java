package org.facens.grupo_6_gameeducator.gamificacao.porta;

import java.util.List;
import org.facens.grupo_6_gameeducator.gamificacao.PosicaoNoRanking;
import org.facens.grupo_6_gameeducator.gamificacao.dominio.Progresso;

/** Porta de saida: onde o progresso de XP dos alunos e guardado. */
public interface RepositorioDeProgresso {

    /** Progresso atual do aluno no curso, ou o progresso inicial (0 XP) se ele ainda nao pontuou. */
    Progresso carregar(Long alunoId, Long cursoId);

    void salvar(Progresso progresso);

    /** Alunos do curso ordenados por XP, do maior para o menor, com a posicao (1..n). */
    List<PosicaoNoRanking> ranking(Long cursoId);
}
