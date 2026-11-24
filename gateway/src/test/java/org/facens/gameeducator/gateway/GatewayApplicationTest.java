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
        assertThat(ids).containsExactlyInAnyOrder("core-agente", "core-api", "core-openapi", "ia-saude", "frontend");
    }

    @Test
    void todaRotaPreservaOHostOriginalParaOCorsDoCore() {
        List<Route> todas = rotas.getRoutes().collectList().block();
        assertThat(todas).allSatisfy(rota -> assertThat(rota.getFilters())
                .anySatisfy(filtro -> assertThat(filtro.toString()).contains("PreserveHostHeader")));
    }

    @Test
    void agenteVemAntesDaApiESpaPorUltimo() {
        List<String> ids = rotas.getRoutes().map(Route::getId).collectList().block();
        assertThat(ids).first().isEqualTo("core-agente");
        assertThat(ids).last().isEqualTo("frontend");
    }
}
