package org.facens.grupo_6_gameeducator.iot;

import io.micrometer.core.instrument.MeterRegistry;
import org.facens.grupo_6_gameeducator.iot.adaptador.ConsumidorIotRabbit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** O consumidor AMQP so existe com mensageria ligada (sem broker em dev/testes, nada tenta conectar). */
@Configuration
public class IotConfig {

    @Bean
    TradutorDeEventoIot tradutorDeEventoIot(JsonMapper json) {
        return new TradutorDeEventoIot(json);
    }

    @Bean
    @ConditionalOnProperty(name = "app.mensageria.habilitada", havingValue = "true")
    ConsumidorIotRabbit consumidorIotRabbit(TradutorDeEventoIot tradutor, RegistroDeEventosIot registro,
                                            MeterRegistry registry) {
        return new ConsumidorIotRabbit(tradutor, registro, registry);
    }
}
