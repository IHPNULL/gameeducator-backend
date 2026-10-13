package org.facens.grupo_6_gameeducator.eventos;

import java.time.Instant;

/** O aluno acertou todos os desafios de uma missao (equivalente a "concluir a atividade/curso" do case). */
public record MissaoConcluida(String eventoId, Long alunoId, Long cursoId, Long missaoId, String tituloMissao,
                              int xpTotalNoCurso, Instant ocorridoEm) implements EventoDeDominio {

    public static final String TIPO = "gamificacao.missao-concluida";

    @Override
    public String tipo() {
        return TIPO;
    }
}
