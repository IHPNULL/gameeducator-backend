package org.facens.grupo_6_gameeducator.eventos;

import java.time.Instant;

/** Evento de laboratorio (IoT) aceito pelo Core e encaminhado ao ecossistema (Desafio 5, RF06). */
public record EventoIotRegistrado(String eventoId, String laboratorio, String dispositivo, String tipoIot,
                                  Long alunoId, String atividade, Instant ocorridoEm) implements EventoDeDominio {

    public static final String TIPO = "iot.evento-registrado";

    @Override
    public String tipo() {
        return TIPO;
    }
}
