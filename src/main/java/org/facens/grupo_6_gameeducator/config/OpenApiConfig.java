package org.facens.grupo_6_gameeducator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gameEducatorOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("GameEducator API")
                        .description("API da plataforma de ensino gamificada — missoes, desafios, XP e ranking.")
                        .version("v1"));
    }
}
