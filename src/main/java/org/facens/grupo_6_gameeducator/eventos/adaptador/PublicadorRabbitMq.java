package org.facens.grupo_6_gameeducator.eventos.adaptador;

import java.nio.charset.StandardCharsets;
import org.facens.grupo_6_gameeducator.eventos.PublicadorDeEventos;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Publica no exchange topic do RabbitMQ (routing key = tipo do evento) com mensagem persistente e
 * publisher confirm: o evento so sai do outbox depois que o broker confirma que o recebeu.
 */
public class PublicadorRabbitMq implements PublicadorDeEventos {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final long timeoutConfirmacaoMs;

    public PublicadorRabbitMq(RabbitTemplate rabbitTemplate, String exchange, long timeoutConfirmacaoMs) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.timeoutConfirmacaoMs = timeoutConfirmacaoMs;
    }

    @Override
    public void publicar(String eventoId, String tipo, String payloadJson) {
        MessageProperties propriedades = new MessageProperties();
        propriedades.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        propriedades.setContentEncoding(StandardCharsets.UTF_8.name());
        propriedades.setMessageId(eventoId);
        propriedades.setType(tipo);
        propriedades.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        Message mensagem = new Message(payloadJson.getBytes(StandardCharsets.UTF_8), propriedades);
        rabbitTemplate.invoke(operacoes -> {
            operacoes.send(exchange, tipo, mensagem);
            operacoes.waitForConfirmsOrDie(timeoutConfirmacaoMs);
            return null;
        });
    }
}
