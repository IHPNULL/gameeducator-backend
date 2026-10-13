package org.facens.grupo_6_gameeducator.iot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("Camada anticorrupcao IoT: payload do dispositivo -> modelo canonico")
class TradutorDeEventoIotTest {

    private static final String ROTA = "gameeducator.lab.lab-01.dispositivo.porta-01.evento";
    private final TradutorDeEventoIot tradutor = new TradutorDeEventoIot(JsonMapper.builder().build());

    private EventoDeLaboratorio traduzir(String rota, String json) {
        return tradutor.traduzir(rota, json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("payload compacto valido vira evento canonico (laboratorio e dispositivo vem do topico)")
    void valido() {
        EventoDeLaboratorio evento = traduzir(ROTA, "{\"id\":\"abc\",\"t\":\"C\",\"a\":42,\"atv\":\"redes-1\",\"ts\":1760000000}");

        assertEquals(new EventoDeLaboratorio("abc", "lab-01", "porta-01", TipoEventoIot.CONCLUSAO_ATIVIDADE, 42L,
                "redes-1", Instant.ofEpochSecond(1760000000)), evento);
        assertNull(traduzir(ROTA, "{\"id\":\"abc\",\"t\":\"P\",\"a\":42,\"ts\":1}").atividade());
        assertEquals(TipoEventoIot.INICIO_ATIVIDADE, traduzir(ROTA, "{\"id\":\"abc\",\"t\":\"I\",\"a\":1,\"ts\":1}").tipo());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "nao-e-json",
            "{\"t\":\"P\",\"a\":1,\"ts\":1}",
            "{\"id\":\" \",\"t\":\"P\",\"a\":1,\"ts\":1}",
            "{\"id\":\"0123456789012345678901234567890123456\",\"t\":\"P\",\"a\":1,\"ts\":1}",
            "{\"id\":\"x\",\"t\":\"Z\",\"a\":1,\"ts\":1}",
            "{\"id\":\"x\",\"t\":\"P\",\"ts\":1}",
            "{\"id\":\"x\",\"t\":\"P\",\"a\":1}",
            "{\"id\":\"x\",\"t\":\"P\",\"a\":\"um\",\"ts\":1}"
    })
    @DisplayName("payload fora do contrato e rejeitado (vai para a DLQ)")
    void payloadInvalido(String json) {
        assertThrows(EventoIotInvalidoException.class, () -> traduzir(ROTA, json));
    }

    @Test
    @DisplayName("topico fora do padrao e rejeitado")
    void topicoInvalido() {
        String json = "{\"id\":\"x\",\"t\":\"P\",\"a\":1,\"ts\":1}";
        assertThrows(EventoIotInvalidoException.class, () -> traduzir("gameeducator.lab.lab-01.status", json));
        assertThrows(EventoIotInvalidoException.class, () -> traduzir(null, json));
    }
}
