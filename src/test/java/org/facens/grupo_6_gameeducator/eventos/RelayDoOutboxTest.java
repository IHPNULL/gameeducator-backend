package org.facens.grupo_6_gameeducator.eventos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import org.facens.grupo_6_gameeducator.eventos.outbox.OutboxEvento;
import org.facens.grupo_6_gameeducator.eventos.outbox.OutboxEventoRepository;
import org.facens.grupo_6_gameeducator.eventos.outbox.RelayDoOutbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/** Desafio 4 - o relay com o broker fora: nada se perde, a ordem e mantida, e tudo sai quando o broker volta. */
@SpringBootTest
@Transactional
@DisplayName("Relay do outbox")
class RelayDoOutboxTest {

    @MockitoBean
    private PublicadorDeEventos publicador;
    @Autowired
    private RelayDoOutbox relay;
    @Autowired
    private OutboxEventoRepository outbox;
    @Autowired
    private MeterRegistry registry;

    @BeforeEach
    void limpar() {
        outbox.deleteAll();
        outbox.save(new OutboxEvento("e1", "gamificacao.missao-concluida", "{\"n\":1}", Instant.parse("2026-10-01T10:00:00Z")));
        outbox.save(new OutboxEvento("e2", "gamificacao.missao-concluida", "{\"n\":2}", Instant.parse("2026-10-01T10:00:01Z")));
        outbox.save(new OutboxEvento("e3", "gamificacao.medalha-conquistada", "{\"n\":3}", Instant.parse("2026-10-01T10:00:02Z")));
    }

    @Test
    @DisplayName("broker fora no 2o evento: publica o 1o, para o lote, registra a falha e mantem o resto pendente")
    void brokerFora() {
        doNothing().when(publicador).publicar(eq("e1"), anyString(), anyString());
        doThrow(new AmqpConnectException(new java.net.ConnectException("Connection refused")))
                .when(publicador).publicar(eq("e2"), anyString(), anyString());

        int publicados = relay.publicarPendentes();

        assertEquals(1, publicados);
        assertNotNull(outbox.findById("e1").orElseThrow().getPublicadoEm());
        OutboxEvento e2 = outbox.findById("e2").orElseThrow();
        assertNull(e2.getPublicadoEm());
        assertEquals(1, e2.getTentativas());
        assertTrue(e2.getUltimoErro().contains("Connection refused"));
        assertEquals(0, outbox.findById("e3").orElseThrow().getTentativas(), "nao tentou depois da falha");
        assertEquals(2.0, registry.get("gameeducator.outbox.pendentes").gauge().value());
    }

    @Test
    @DisplayName("broker de volta: os pendentes saem em ordem de criacao e o outbox zera")
    void brokerDeVolta() {
        int publicados = relay.publicarPendentes();

        assertEquals(3, publicados);
        var ordem = org.mockito.Mockito.inOrder(publicador);
        ordem.verify(publicador).publicar("e1", "gamificacao.missao-concluida", "{\"n\":1}");
        ordem.verify(publicador).publicar("e2", "gamificacao.missao-concluida", "{\"n\":2}");
        ordem.verify(publicador).publicar("e3", "gamificacao.medalha-conquistada", "{\"n\":3}");
        assertEquals(0L, outbox.countByPublicadoEmIsNull());
        double sucessos = registry.get("gameeducator.eventos.publicados").tag("resultado", "sucesso").counters().stream()
                .mapToDouble(io.micrometer.core.instrument.Counter::count).sum();
        assertTrue(sucessos >= 3, "sucessos = " + sucessos);
    }

    @Test
    @DisplayName("o metodo agendado executa um ciclo do relay")
    void agendado() {
        relay.agendado();

        verify(publicador).publicar("e3", "gamificacao.medalha-conquistada", "{\"n\":3}");
    }
}
