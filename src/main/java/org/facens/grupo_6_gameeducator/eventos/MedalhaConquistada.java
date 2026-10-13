package org.facens.grupo_6_gameeducator.eventos;

import java.time.Instant;

public record MedalhaConquistada(String eventoId, Long alunoId, Long cursoId, int marcoXp, Instant ocorridoEm)
        implements EventoDeDominio {

    public static final String TIPO = "gamificacao.medalha-conquistada";

    @Override
    public String tipo() {
        return TIPO;
    }
}
