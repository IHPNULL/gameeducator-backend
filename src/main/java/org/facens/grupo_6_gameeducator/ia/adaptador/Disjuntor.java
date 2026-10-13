package org.facens.grupo_6_gameeducator.ia.adaptador;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Circuit breaker minimo. Depois de {@code limiteFalhas} falhas seguidas, abre e recusa chamadas por
 * {@code espera}: com a IA fora do ar, o Core responde o fallback em microssegundos em vez de
 * esperar o timeout a cada requisicao. Depois da espera, deixa uma chamada de teste (meio-aberto).
 *
 * <p>Escolha consciente (ADR-B1-003): ~40 linhas proprias em vez de uma biblioteca, porque so ha uma
 * dependencia remota protegida. Com mais integracoes, trocar por Resilience4j.
 */
public class Disjuntor {

    public enum Estado { FECHADO, ABERTO, MEIO_ABERTO }

    private final int limiteFalhas;
    private final Duration espera;
    private final Clock relogio;

    private Estado estado = Estado.FECHADO;
    private int falhasSeguidas;
    private Instant abertoEm = Instant.MIN;

    public Disjuntor(int limiteFalhas, Duration espera, Clock relogio) {
        if (limiteFalhas < 1) {
            throw new IllegalArgumentException("limiteFalhas precisa ser >= 1");
        }
        this.limiteFalhas = limiteFalhas;
        this.espera = espera;
        this.relogio = relogio;
    }

    public synchronized boolean permiteChamada() {
        if (estado == Estado.ABERTO && !relogio.instant().isBefore(abertoEm.plus(espera))) {
            estado = Estado.MEIO_ABERTO;
        }
        return estado != Estado.ABERTO;
    }

    public synchronized void registrarSucesso() {
        falhasSeguidas = 0;
        estado = Estado.FECHADO;
    }

    public synchronized void registrarFalha() {
        falhasSeguidas++;
        if (estado == Estado.MEIO_ABERTO || falhasSeguidas >= limiteFalhas) {
            estado = Estado.ABERTO;
            abertoEm = relogio.instant();
        }
    }

    public synchronized Estado estado() {
        return estado;
    }
}
