package org.facens.gameeducator.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway: unica porta exposta da plataforma. Roteia a SPA, a API do Core e a saude da IA;
 * os demais containers (Core, IA, banco, broker) ficam so na rede interna do compose.
 * As rotas estao em application.yml.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
