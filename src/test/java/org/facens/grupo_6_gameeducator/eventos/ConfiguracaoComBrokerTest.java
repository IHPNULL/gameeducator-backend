package org.facens.grupo_6_gameeducator.eventos;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.facens.grupo_6_gameeducator.eventos.adaptador.EmissorSincronoHttp;
import org.facens.grupo_6_gameeducator.eventos.adaptador.PublicadorRabbitMq;
import org.facens.grupo_6_gameeducator.iot.adaptador.ConsumidorIotRabbit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * Montagem "de producao" (mensageria ligada) e do lab sincrono: os beans certos sao escolhidos so por
 * propriedade. O listener nao inicia (auto-startup=false) porque nao ha broker no teste.
 */
@SpringBootTest(properties = {
        "app.mensageria.habilitada=true",
        "app.eventos.modo=sincrono",
        "app.eventos.relay.habilitado=true",
        "app.eventos.relay.intervalo=1h",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "management.health.rabbit.enabled=false"
})
@DisplayName("Configuracao com broker e modo sincrono")
class ConfiguracaoComBrokerTest {

    @Autowired
    private ApplicationContext contexto;

    @Test
    @DisplayName("mensageria=true -> RabbitMQ + consumidor IoT; modo=sincrono -> REST direto")
    void beansPorPropriedade() {
        assertInstanceOf(PublicadorRabbitMq.class, contexto.getBean(PublicadorDeEventos.class));
        assertInstanceOf(EmissorSincronoHttp.class, contexto.getBean(EmissorDeEventos.class));
        assertNotNull(contexto.getBean(ConsumidorIotRabbit.class));
    }
}
