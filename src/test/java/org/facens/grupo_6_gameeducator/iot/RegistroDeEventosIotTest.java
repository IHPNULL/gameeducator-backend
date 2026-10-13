package org.facens.grupo_6_gameeducator.iot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.facens.grupo_6_gameeducator.eventos.EventoIotRegistrado;
import org.facens.grupo_6_gameeducator.eventos.outbox.OutboxEventoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** RF06 - idempotencia: o mesmo evento entregue duas vezes (QoS 1 / reenvio apos reconexao) conta uma vez. */
@SpringBootTest
@Transactional
@DisplayName("Registro idempotente de eventos IoT")
class RegistroDeEventosIotTest {

    @Autowired
    private RegistroDeEventosIot registro;
    @Autowired
    private OutboxEventoRepository outbox;

    private static EventoDeLaboratorio evento(String id) {
        return new EventoDeLaboratorio(id, "lab-teste", "porta-01", TipoEventoIot.PRESENCA, 42L, null,
                Instant.parse("2026-10-01T10:00:00Z"));
    }

    @Test
    @DisplayName("primeira entrega e aceita e encaminhada ao ecossistema; a repeticao e ignorada")
    void duplicadoIgnorado() {
        assertEquals(RegistroDeEventosIot.Resultado.ACEITO, registro.registrar(evento("iot-1")));
        assertEquals(RegistroDeEventosIot.Resultado.DUPLICADO, registro.registrar(evento("iot-1")));
        assertEquals(RegistroDeEventosIot.Resultado.ACEITO, registro.registrar(evento("iot-2")));

        assertEquals(2, registro.total("lab-teste"));
        assertEquals(2, registro.recentes("lab-teste").size());
        assertTrue(outbox.findById("iot-1").orElseThrow().getTipo().equals(EventoIotRegistrado.TIPO));
        assertTrue(outbox.findById("iot-1").orElseThrow().getPayload().contains("\"laboratorio\":\"lab-teste\""));
    }
}
