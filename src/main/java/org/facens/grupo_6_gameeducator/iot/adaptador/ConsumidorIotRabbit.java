package org.facens.grupo_6_gameeducator.iot.adaptador;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.facens.grupo_6_gameeducator.iot.EventoIotInvalidoException;
import org.facens.grupo_6_gameeducator.iot.RegistroDeEventosIot;
import org.facens.grupo_6_gameeducator.iot.TradutorDeEventoIot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Adaptador de entrada: fila AMQP core.iot.eventos (alimentada pelo plugin MQTT do RabbitMQ).
 * Ack so depois de gravar (falha de banco = mensagem volta para a fila). Payload invalido vai para a DLQ.
 */
public class ConsumidorIotRabbit {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorIotRabbit.class);

    private final TradutorDeEventoIot tradutor;
    private final RegistroDeEventosIot registro;
    private final MeterRegistry registry;

    public ConsumidorIotRabbit(TradutorDeEventoIot tradutor, RegistroDeEventosIot registro, MeterRegistry registry) {
        this.tradutor = tradutor;
        this.registro = registro;
        this.registry = registry;
    }

    @RabbitListener(queues = "${app.iot.fila:core.iot.eventos}")
    public void receber(Message mensagem) {
        String routingKey = mensagem.getMessageProperties().getReceivedRoutingKey();
        try {
            RegistroDeEventosIot.Resultado resultado = registro.registrar(tradutor.traduzir(routingKey, mensagem.getBody()));
            contar(resultado.name().toLowerCase());
        } catch (EventoIotInvalidoException e) {
            contar("invalido");
            log.warn("Evento IoT invalido em {}: {}", routingKey, e.getMessage());
            throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
        } catch (DataIntegrityViolationException e) {
            // duas entregas do mesmo evento processadas em paralelo: a PK barrou a segunda
            contar("duplicado");
        }
    }

    private void contar(String resultado) {
        Counter.builder("gameeducator.iot.eventos")
                .description("Eventos IoT recebidos pelo Core por resultado (aceito, duplicado, invalido)")
                .tag("resultado", resultado)
                .register(registry)
                .increment();
    }
}
