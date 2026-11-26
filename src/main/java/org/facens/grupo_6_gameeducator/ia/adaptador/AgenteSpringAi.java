package org.facens.grupo_6_gameeducator.ia.adaptador;

import org.facens.grupo_6_gameeducator.ia.IaIndisponivelException;
import org.facens.grupo_6_gameeducator.ia.OrquestradorDeAgente;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;

/**
 * Agente com Spring AI: o {@link ChatClient} manda instrucoes, historico e ferramentas ao LLM; o modelo
 * escolhe as ferramentas e o Spring AI as executa no loop de tool calling ate o texto final.
 *
 * <p>Memoria: {@link MessageChatMemoryAdvisor} guarda as ultimas mensagens por conversa (usuario + curso).
 * Falhas do LLM (fora do ar, modelo ainda baixando, timeout) viram {@link IaIndisponivelException}.
 * As ferramentas devolvem recusas como texto (ex.: "aluno nao matriculado"), que o modelo repassa ao usuario.
 */
public class AgenteSpringAi implements OrquestradorDeAgente {

    private static final Logger log = LoggerFactory.getLogger(AgenteSpringAi.class);
    static final String SEM_RESPOSTA = "Nao consegui formular uma resposta. Tente reformular a pergunta.";

    private final ChatClient chatClient;

    public AgenteSpringAi(ChatClient.Builder builder, ChatMemory memoria) {
        this.chatClient = builder.clone()
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memoria).build())
                .build();
    }

    @Override
    public String conduzir(PedidoAoAgente pedido) {
        String resposta;
        try {
            resposta = chatClient.prompt()
                    .system(pedido.instrucoes())
                    .user(pedido.mensagem())
                    .tools(pedido.ferramentas().toArray())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, pedido.conversaId()))
                    .call()
                    .content();
        } catch (RuntimeException e) {
            log.warn("Agente de IA indisponivel: {}", e.toString());
            throw new IaIndisponivelException("Agente de IA indisponivel: " + e.getMessage(), e);
        }
        return resposta == null || resposta.isBlank() ? SEM_RESPOSTA : resposta.strip();
    }
}
