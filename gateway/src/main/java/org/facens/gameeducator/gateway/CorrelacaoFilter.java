package org.facens.gameeducator.gateway;

import java.util.UUID;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Garante um X-Correlation-Id em toda requisicao que passa pelo gateway: reaproveita o do cliente
 * ou gera um novo, repassa ao servico de destino e devolve na resposta, para rastrear uma chamada
 * entre gateway, Core e IA nos logs.
 */
@Component
public class CorrelacaoFilter implements GlobalFilter, Ordered {

    public static final String CABECALHO = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String recebido = exchange.getRequest().getHeaders().getFirst(CABECALHO);
        String id = (recebido == null || recebido.isBlank()) ? UUID.randomUUID().toString() : recebido;
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> h.set(CABECALHO, id))
                .build();
        exchange.getResponse().getHeaders().set(CABECALHO, id);
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
