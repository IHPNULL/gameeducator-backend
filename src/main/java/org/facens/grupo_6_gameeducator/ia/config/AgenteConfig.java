package org.facens.grupo_6_gameeducator.ia.config;

import org.facens.grupo_6_gameeducator.ia.OrquestradorDeAgente;
import org.facens.grupo_6_gameeducator.ia.adaptador.AgenteDesabilitado;
import org.facens.grupo_6_gameeducator.ia.adaptador.AgenteSpringAi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RF04 - montagem do agente. Com app.agente.habilitado=true usa Spring AI (modelo em spring.ai.ollama.*);
 * senao, o agente fica desabilitado e o resto do Core segue igual (RNF04: a IA evolui sem afetar o cadastro).
 */
@Configuration
public class AgenteConfig {

    @Bean
    public OrquestradorDeAgente orquestradorDeAgente(@Value("${app.agente.habilitado:false}") boolean habilitado,
                                              @Value("${app.agente.memoria.mensagens:20}") int mensagensNaMemoria,
                                              ObjectProvider<ChatClient.Builder> builder) {
        if (!habilitado) {
            return new AgenteDesabilitado();
        }
        MessageWindowChatMemory memoria = MessageWindowChatMemory.builder().maxMessages(mensagensNaMemoria).build();
        return new AgenteSpringAi(builder.getObject(), memoria);
    }
}
