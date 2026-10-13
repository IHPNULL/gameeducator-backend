package org.facens.grupo_6_gameeducator.eventos.outbox;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import org.facens.grupo_6_gameeducator.eventos.PublicadorDeEventos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Publica os eventos pendentes do outbox, em ordem. Se o broker estiver fora, para o lote e tenta de novo
 * no proximo ciclo: os eventos se acumulam no banco (metrica gameeducator_outbox_pendentes) e nada se perde.
 * Entrega e "pelo menos uma vez": consumidores deduplicam por eventoId.
 */
public class RelayDoOutbox {

    private static final Logger log = LoggerFactory.getLogger(RelayDoOutbox.class);

    private final OutboxEventoRepository repository;
    private final PublicadorDeEventos publicador;
    private final MeterRegistry registry;
    private final Clock relogio;
    private final TransactionTemplate transacao;

    public RelayDoOutbox(OutboxEventoRepository repository, PublicadorDeEventos publicador,
                         MeterRegistry registry, Clock relogio, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.publicador = publicador;
        this.registry = registry;
        this.relogio = relogio;
        this.transacao = new TransactionTemplate(transactionManager);
        Gauge.builder("gameeducator.outbox.pendentes", repository, OutboxEventoRepository::countByPublicadoEmIsNull)
                .description("Eventos gravados no outbox e ainda nao confirmados pelo broker")
                .register(registry);
    }

    @Scheduled(fixedDelayString = "${app.eventos.relay.intervalo:2s}")
    public void agendado() {
        publicarPendentes();
    }

    /**
     * Um ciclo do relay, em transacao propria (TransactionTemplate e nao @Transactional: o metodo e chamado
     * pelo proprio {@link #agendado()}, e auto-invocacao nao passa pelo proxy).
     *
     * @return quantos eventos foram publicados neste ciclo
     */
    public int publicarPendentes() {
        Integer publicados = transacao.execute(status -> publicarLote());
        return publicados == null ? 0 : publicados;
    }

    private int publicarLote() {
        int publicados = 0;
        for (OutboxEvento evento : repository.findTop50ByPublicadoEmIsNullOrderByCriadoEmAsc()) {
            try {
                publicador.publicar(evento.getEventoId(), evento.getTipo(), evento.getPayload());
                evento.marcarPublicado(relogio.instant());
                contar(evento.getTipo(), "sucesso");
                publicados++;
            } catch (RuntimeException e) {
                evento.registrarFalha(e.getMessage());
                contar(evento.getTipo(), "falha");
                log.warn("Broker indisponivel; {} fica pendente no outbox: {}", evento.getEventoId(), e.getMessage());
                break;
            }
        }
        return publicados;
    }

    private void contar(String tipo, String resultado) {
        Counter.builder("gameeducator.eventos.publicados")
                .description("Tentativas de publicacao de eventos do outbox no broker")
                .tag("tipo", tipo)
                .tag("resultado", resultado)
                .register(registry)
                .increment();
    }
}
