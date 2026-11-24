package org.facens.grupo_6_gameeducator.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.facens.grupo_6_gameeducator.ia.OrquestradorDeAgente.PedidoAoAgente;
import org.facens.grupo_6_gameeducator.ia.adaptador.AgenteDesabilitado;
import org.facens.grupo_6_gameeducator.ia.adaptador.AgenteSpringAi;
import org.facens.grupo_6_gameeducator.ia.config.AgenteConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * Adaptador Spring AI do agente (RF04) com um ChatModel de mentira: verifica o que chega ao modelo
 * (instrucoes, mensagem, ferramentas, historico) e a traducao de falhas para IaIndisponivelException.
 */
@DisplayName("Agente com Spring AI")
class AgenteSpringAiTest {

    static class FerramentasDeTeste {
        @Tool(description = "Diz oi")
        public String dizerOi() {
            return "oi";
        }
    }

    /** ChatModel que guarda os prompts recebidos e responde com a funcao dada. */
    static class ModeloFalso implements ChatModel {
        final List<Prompt> prompts = new ArrayList<>();
        final Function<Prompt, String> resposta;

        ModeloFalso(Function<Prompt, String> resposta) {
            this.resposta = resposta;
        }

        /** Como o OllamaChatModel: opcoes com suporte a ferramentas, senao o ChatClient nao as envia. */
        @Override
        public ChatOptions getDefaultOptions() {
            return ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatOptions getOptions() {
            return getDefaultOptions();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);
            return new ChatResponse(List.of(new Generation(new AssistantMessage(resposta.apply(prompt)))));
        }
    }

    private static AgenteSpringAi agente(ChatModel modelo) {
        return new AgenteSpringAi(ChatClient.builder(modelo), MessageWindowChatMemory.builder().maxMessages(10).build());
    }

    private static PedidoAoAgente pedido(String conversa, String mensagem) {
        return new PedidoAoAgente(conversa, "Voce e um agente de teste", mensagem, List.of(new FerramentasDeTeste()));
    }

    @Test
    @DisplayName("envia instrucoes, mensagem e as ferramentas @Tool ao modelo e devolve o texto")
    void enviaPromptComFerramentas() {
        ModeloFalso modelo = new ModeloFalso(p -> "  Ola!  ");

        String resposta = agente(modelo).conduzir(pedido("c1", "oi"));

        assertEquals("Ola!", resposta);
        Prompt prompt = modelo.prompts.get(0);
        assertEquals("Voce e um agente de teste", prompt.getSystemMessage().getText());
        assertEquals("oi", prompt.getUserMessage().getText());
        ToolCallingChatOptions opcoes = assertInstanceOf(ToolCallingChatOptions.class, prompt.getOptions());
        assertEquals(List.of("dizerOi"), opcoes.getToolCallbacks().stream()
                .map(ToolCallback::getToolDefinition).map(d -> d.name()).toList());
    }

    @Test
    @DisplayName("mantem memoria por conversa: a 2a mensagem leva o historico, outra conversa nao")
    void memoriaPorConversa() {
        ModeloFalso modelo = new ModeloFalso(p -> "resposta");
        AgenteSpringAi agente = agente(modelo);

        agente.conduzir(pedido("c1", "primeira"));
        agente.conduzir(pedido("c1", "segunda"));
        agente.conduzir(pedido("c2", "outra"));

        List<String> segunda = textos(modelo.prompts.get(1).getInstructions());
        assertTrue(segunda.containsAll(List.of("primeira", "resposta", "segunda")), segunda.toString());
        assertTrue(!textos(modelo.prompts.get(2).getInstructions()).contains("primeira"));
    }

    @Test
    @DisplayName("resposta vazia vira mensagem padrao; falha do modelo vira IaIndisponivelException")
    void respostaVaziaEFalha() {
        assertTrue(agente(new ModeloFalso(p -> " ")).conduzir(pedido("c", "oi")).startsWith("Nao consegui"));

        ChatModel quebrado = new ModeloFalso(p -> {
            throw new IllegalStateException("connection refused");
        });
        IaIndisponivelException erro = assertThrows(IaIndisponivelException.class,
                () -> agente(quebrado).conduzir(pedido("c", "oi")));
        assertTrue(erro.getMessage().contains("connection refused"));
    }

    @Test
    @DisplayName("configuracao: desligado usa AgenteDesabilitado (sempre indisponivel); ligado usa Spring AI")
    void configuracao() {
        AgenteConfig config = new AgenteConfig();
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("builder", ChatClient.builder(new ModeloFalso(p -> "ok")));

        OrquestradorDeAgente desligado = config.orquestradorDeAgente(false, 10, beans.getBeanProvider(ChatClient.Builder.class));
        OrquestradorDeAgente ligado = config.orquestradorDeAgente(true, 10, beans.getBeanProvider(ChatClient.Builder.class));

        assertInstanceOf(AgenteDesabilitado.class, desligado);
        assertThrows(IaIndisponivelException.class, () -> desligado.conduzir(pedido("c", "oi")));
        assertInstanceOf(AgenteSpringAi.class, ligado);
        assertEquals("ok", ligado.conduzir(pedido("c", "oi")));
    }

    private static List<String> textos(List<Message> mensagens) {
        return mensagens.stream().map(Message::getText).toList();
    }
}
