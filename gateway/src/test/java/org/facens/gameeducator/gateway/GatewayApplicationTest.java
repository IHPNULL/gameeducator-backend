package org.facens.gameeducator.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

@SpringBootTest
class GatewayApplicationTest {

    @Autowired
    RouteLocator rotas;

    @Test
    void carregaAsRotasDoCoreDaIaEDaSpa() {
        List<String> ids = rotas.getRoutes().map(Route::getId).collectList().block();
        assertThat(ids).containsExactlyInAnyOrder("core-api", "core-openapi", "ia-saude", "frontend");
    }

    @Test
    void spaEhAUltimaRotaParaNaoEngolirAApi() {
        List<String> ids = rotas.getRoutes().map(Route::getId).collectList().block();
        assertThat(ids).last().isEqualTo("frontend");
    }
}
