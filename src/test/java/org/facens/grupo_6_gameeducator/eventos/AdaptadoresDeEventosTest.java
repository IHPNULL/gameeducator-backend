package org.facens.grupo_6_gameeducator.eventos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.facens.grupo_6_gameeducator.eventos.adaptador.EmissorSincronoHttp;
import org.facens.grupo_6_gameeducator.eventos.adaptador.PublicadorEmLog;
import org.facens.grupo_6_gameeducator.eventos.adaptador.PublicadorRabbitMq;
import org.facens.grupo_6_gameeducator.eventos.outbox.OutboxEvento;
import org.facens.grupo_6_gameeducator.exception.ServicoIndisponivelException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("Adaptadores de eventos (RabbitMQ, REST sincrono, log)")
class AdaptadoresDeEventosTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final MissaoConcluida evento = new MissaoConcluida("e-1", 2L, 10L, 100L, "Fracoes", 60,
            Instant.parse("2026-10-01T10:00:00Z"));

    @Test
    @DisplayName("RabbitMQ: mensagem persistente, JSON, messageId = eventoId, routing key = tipo, com publisher confirm")
    @SuppressWarnings("unchecked")
    void rabbitMq() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        RabbitOperations operacoes = mock(RabbitOperations.class);
        when(template.invoke(any(RabbitOperations.OperationsCallback.class)))
                .thenAnswer(inv -> ((RabbitOperations.OperationsCallback<Object>) inv.getArgument(0)).doInRabbit(operacoes));

        new PublicadorRabbitMq(template, "gameeducator.eventos", 5000).publicar("e-1", MissaoConcluida.TIPO, "{\"a\":1}");

        ArgumentCaptor<Message> mensagem = ArgumentCaptor.forClass(Message.class);
        verify(operacoes).send(org.mockito.ArgumentMatchers.eq("gameeducator.eventos"),
                org.mockito.ArgumentMatchers.eq(MissaoConcluida.TIPO), mensagem.capture());
        verify(operacoes).waitForConfirmsOrDie(5000);
        assertEquals("e-1", mensagem.getValue().getMessageProperties().getMessageId());
        assertEquals(MessageDeliveryMode.PERSISTENT, mensagem.getValue().getMessageProperties().getDeliveryMode());
        assertEquals("application/json", mensagem.getValue().getMessageProperties().getContentType());
        assertEquals("{\"a\":1}", new String(mensagem.getValue().getBody(), StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("REST sincrono: com o servico de notificacao no ar, envia o evento")
    void sincronoNoAr() throws IOException {
        AtomicReference<String> recebido = new AtomicReference<>();
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/notificacoes", troca -> {
            recebido.set(troca.getRequestHeaders().getFirst("X-Evento-Tipo") + " "
                    + new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            troca.sendResponseHeaders(202, -1);
            troca.close();
        });
        servidor.start();
        try {
            emissor(servidor.getAddress().getPort()).emitir(evento);
        } finally {
            servidor.stop(0);
        }

        assertTrue(recebido.get().startsWith(MissaoConcluida.TIPO + " {"));
        assertTrue(recebido.get().contains("\"eventoId\":\"e-1\""));
        assertTrue(recebido.get().contains("\"ocorridoEm\":\"2026-10-01T10:00:00Z\""), "Instant em ISO-8601");
    }

    @Test
    @DisplayName("REST sincrono: com o servico de notificacao fora, a operacao do aluno falha (503)")
    void sincronoFora() throws IOException {
        int portaLivre;
        try (ServerSocket socket = new ServerSocket(0)) {
            portaLivre = socket.getLocalPort();
        }

        assertThrows(ServicoIndisponivelException.class, () -> emissor(portaLivre).emitir(evento));
    }

    @Test
    @DisplayName("log: apenas registra (dev sem broker)")
    void log() {
        new PublicadorEmLog().publicar("e-1", MissaoConcluida.TIPO, "{}");
    }

    @Test
    @DisplayName("outbox: falha registra erro truncado; publicacao limpa o erro")
    void linhaDoOutbox() {
        OutboxEvento linha = new OutboxEvento("e-1", "t", "{}", Instant.now());
        linha.registrarFalha("x".repeat(800));
        assertEquals(500, linha.getUltimoErro().length());
        linha.registrarFalha(null);
        assertNull(linha.getUltimoErro());
        linha.marcarPublicado(Instant.now());
        assertEquals(3, linha.getTentativas());
        assertTrue(linha.publicado());
        assertNull(linha.getUltimoErro());
    }

    private EmissorSincronoHttp emissor(int porta) {
        return new EmissorSincronoHttp(RestClient.builder().baseUrl("http://127.0.0.1:" + porta).build(), json);
    }
}
