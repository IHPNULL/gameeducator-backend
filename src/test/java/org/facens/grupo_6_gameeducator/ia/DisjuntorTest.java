package org.facens.grupo_6_gameeducator.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.facens.grupo_6_gameeducator.ia.adaptador.Disjuntor;
import org.facens.grupo_6_gameeducator.ia.adaptador.Disjuntor.Estado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Disjuntor da integracao com a IA")
class DisjuntorTest {

    /** Relogio controlado pelo teste. */
    private static final class RelogioManual extends Clock {
        private Instant agora = Instant.parse("2026-10-01T10:00:00Z");

        void avancar(Duration d) {
            agora = agora.plus(d);
        }

        @Override
        public Instant instant() {
            return agora;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    private final RelogioManual relogio = new RelogioManual();
    private final Disjuntor disjuntor = new Disjuntor(3, Duration.ofSeconds(15), relogio);

    @Test
    @DisplayName("abre apos 3 falhas seguidas e recusa chamadas durante a espera")
    void abreAposFalhasSeguidas() {
        disjuntor.registrarFalha();
        disjuntor.registrarFalha();
        assertTrue(disjuntor.permiteChamada(), "2 falhas ainda nao abrem");

        disjuntor.registrarFalha();

        assertEquals(Estado.ABERTO, disjuntor.estado());
        assertFalse(disjuntor.permiteChamada());
    }

    @Test
    @DisplayName("sucesso zera a contagem de falhas")
    void sucessoZeraFalhas() {
        disjuntor.registrarFalha();
        disjuntor.registrarFalha();
        disjuntor.registrarSucesso();
        disjuntor.registrarFalha();

        assertEquals(Estado.FECHADO, disjuntor.estado());
    }

    @Test
    @DisplayName("apos a espera fica meio-aberto: sucesso fecha, falha reabre")
    void meioAberto() {
        for (int i = 0; i < 3; i++) {
            disjuntor.registrarFalha();
        }
        relogio.avancar(Duration.ofSeconds(15));

        assertTrue(disjuntor.permiteChamada());
        assertEquals(Estado.MEIO_ABERTO, disjuntor.estado());
        disjuntor.registrarFalha();
        assertEquals(Estado.ABERTO, disjuntor.estado(), "uma falha no meio-aberto reabre");

        relogio.avancar(Duration.ofSeconds(15));
        assertTrue(disjuntor.permiteChamada());
        disjuntor.registrarSucesso();
        assertEquals(Estado.FECHADO, disjuntor.estado());
    }

    @Test
    @DisplayName("limite de falhas precisa ser positivo")
    void limiteInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new Disjuntor(0, Duration.ofSeconds(1), relogio));
    }
}
