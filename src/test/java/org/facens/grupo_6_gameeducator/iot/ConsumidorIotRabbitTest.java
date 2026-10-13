package org.facens.grupo_6_gameeducator.iot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import org.facens.grupo_6_gameeducator.iot.adaptador.ConsumidorIotRabbit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("Consumidor AMQP da fila IoT")
class ConsumidorIotRabbitTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final RegistroDeEventosIot registro = mock(RegistroDeEventosIot.class);
    private final ConsumidorIotRabbit consumidor =
            new ConsumidorIotRabbit(new TradutorDeEventoIot(JsonMapper.builder().build()), registro, registry);

    private static Message mensagem(String corpo) {
        MessageProperties propriedades = new MessageProperties();
        propriedades.setReceivedRoutingKey("gameeducator.lab.lab-01.dispositivo.porta-01.evento");
        return new Message(corpo.getBytes(StandardCharsets.UTF_8), propriedades);
    }

    private double contador(String resultado) {
        return registry.counter("gameeducator.iot.eventos", "resultado", resultado).count();
    }

    @Test
    @DisplayName("aceito, duplicado (logico e por PK) e invalido sao contados; invalido vai para a DLQ")
    void resultados() {
        String valido = "{\"id\":\"x\",\"t\":\"P\",\"a\":1,\"ts\":1}";
        when(registro.registrar(any()))
                .thenReturn(RegistroDeEventosIot.Resultado.ACEITO)
                .thenReturn(RegistroDeEventosIot.Resultado.DUPLICADO)
                .thenThrow(new DataIntegrityViolationException("pk"));

        consumidor.receber(mensagem(valido));
        consumidor.receber(mensagem(valido));
        consumidor.receber(mensagem(valido));
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumidor.receber(mensagem("lixo")));

        assertEquals(1, contador("aceito"));
        assertEquals(2, contador("duplicado"));
        assertEquals(1, contador("invalido"));
    }
}
