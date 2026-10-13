package org.facens.grupo_6_gameeducator.ia.config;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import org.facens.grupo_6_gameeducator.ia.ServicoDeIa;
import org.facens.grupo_6_gameeducator.ia.adaptador.ClienteHttpIa;
import org.facens.grupo_6_gameeducator.ia.adaptador.Disjuntor;
import org.facens.grupo_6_gameeducator.ia.adaptador.ServicoDeIaProtegido;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Montagem da integracao com a IA: URL, timeouts e disjuntor vem de propriedades (app.ia.*). */
@Configuration
public class IaConfig {

    @Bean
    Disjuntor disjuntorDaIa(@Value("${app.ia.disjuntor.limite-falhas:3}") int limiteFalhas,
                            @Value("${app.ia.disjuntor.espera:15s}") Duration espera) {
        return new Disjuntor(limiteFalhas, espera, Clock.systemUTC());
    }

    @Bean
    ServicoDeIa servicoDeIa(RestClient.Builder builder,
                            Disjuntor disjuntorDaIa,
                            MeterRegistry registry,
                            @Value("${app.ia.url:http://localhost:8000}") String url,
                            @Value("${app.ia.timeout-conexao:300ms}") Duration timeoutConexao,
                            @Value("${app.ia.timeout-leitura:2s}") Duration timeoutLeitura) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(timeoutConexao);
        fabrica.setReadTimeout(timeoutLeitura);
        RestClient restClient = builder.clone().baseUrl(url).requestFactory(fabrica).build();
        return new ServicoDeIaProtegido(new ClienteHttpIa(restClient), disjuntorDaIa, registry);
    }
}
