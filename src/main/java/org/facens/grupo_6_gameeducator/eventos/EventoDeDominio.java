package org.facens.grupo_6_gameeducator.eventos;

import java.time.Instant;

/** Fato de negocio ja ocorrido. {@link #eventoId()} e a chave de idempotencia dos consumidores. */
public sealed interface EventoDeDominio permits MissaoConcluida, MedalhaConquistada, EventoIotRegistrado {

    String eventoId();

    /** Tipo do evento = routing key no RabbitMQ (ex.: gamificacao.missao-concluida). */
    String tipo();

    Instant ocorridoEm();
}
