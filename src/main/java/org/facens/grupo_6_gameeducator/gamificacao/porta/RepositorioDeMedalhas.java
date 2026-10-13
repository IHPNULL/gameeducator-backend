package org.facens.grupo_6_gameeducator.gamificacao.porta;

import java.util.List;
import java.util.Set;
import org.facens.grupo_6_gameeducator.gamificacao.MedalhaDoAluno;

/** Porta de saida: onde as medalhas concedidas sao guardadas. */
public interface RepositorioDeMedalhas {

    Set<Integer> marcosConquistados(Long alunoId, Long cursoId);

    void registrar(Long alunoId, Long cursoId, int marcoXp);

    /** Medalhas do aluno no curso, do menor para o maior marco. */
    List<MedalhaDoAluno> listar(Long alunoId, Long cursoId);
}
