package org.facens.grupo_6_gameeducator.eventos.config;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import org.facens.grupo_6_gameeducator.eventos.EmissorDeEventos;
import org.facens.grupo_6_gameeducator.eventos.PublicadorDeEventos;
import org.facens.grupo_6_gameeducator.eventos.adaptador.EmissorSincronoHttp;
import org.facens.grupo_6_gameeducator.eventos.adaptador.PublicadorEmLog;
import org.facens.grupo_6_gameeducator.eventos.adaptador.PublicadorRabbitMq;
import org.facens.grupo_6_gameeducator.eventos.outbox.EmissorViaOutbox;
import org.facens.grupo_6_gameeducator.eventos.outbox.OutboxEventoRepository;
import org.facens.grupo_6_gameeducator.eventos.outbox.RelayDoOutbox;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Montagem dos eventos:
 * <ul>
 *   <li>{@code app.eventos.modo} = assincrono (outbox, padrao) | sincrono (REST direto, so lab);</li>
 *   <li>{@code app.mensageria.habilitada} = true publica no RabbitMQ; false so loga (dev/testes);</li>
 *   <li>{@code app.eventos.relay.habilitado} = liga o agendamento do relay do outbox.</li>
 * </ul>
 */
@Configuration
public class EventosConfig {

    @Bean
    @ConditionalOnProperty(name = "app.eventos.modo", havingValue = "assincrono", matchIfMissing = true)
    EmissorDeEventos emissorViaOutbox(OutboxEventoRepository repository, JsonMapper json) {
        return new EmissorViaOutbox(repository, json);
    }

    @Bean
    @ConditionalOnProperty(name = "app.eventos.modo", havingValue = "sincrono")
    EmissorDeEventos emissorSincronoHttp(RestClient.Builder builder, JsonMapper json,
                                         @Value("${app.notificacao.url:http://localhost:8085}") String url) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(500));
        fabrica.setReadTimeout(Duration.ofSeconds(2));
        return new EmissorSincronoHttp(builder.clone().baseUrl(url).requestFactory(fabrica).build(), json);
    }

    @Bean
    @ConditionalOnProperty(name = "app.mensageria.habilitada", havingValue = "true")
    PublicadorDeEventos publicadorRabbitMq(RabbitTemplate rabbitTemplate,
                                           @Value("${app.eventos.exchange:gameeducator.eventos}") String exchange) {
        return new PublicadorRabbitMq(rabbitTemplate, exchange, 5000);
    }

    @Bean
    @ConditionalOnProperty(name = "app.mensageria.habilitada", havingValue = "false", matchIfMissing = true)
    PublicadorDeEventos publicadorEmLog() {
        return new PublicadorEmLog();
    }

    @Bean
    RelayDoOutbox relayDoOutbox(OutboxEventoRepository repository, PublicadorDeEventos publicador,
                                MeterRegistry registry, PlatformTransactionManager transactionManager) {
        return new RelayDoOutbox(repository, publicador, registry, Clock.systemUTC(), transactionManager);
    }

    /** O @Scheduled do relay so dispara com o agendamento ligado. */
    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(name = "app.eventos.relay.habilitado", havingValue = "true")
    static class AgendamentoDoRelay {
    }
}
