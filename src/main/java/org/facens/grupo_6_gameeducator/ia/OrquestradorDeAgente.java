package org.facens.grupo_6_gameeducator.ia;

import java.util.List;

/**
 * RF04 - porta do agente de IA: conduz uma conversa com um LLM que pode chamar ferramentas.
 *
 * <p>Os services montam as instrucoes e as ferramentas (objetos com metodos {@code @Tool}) ja presas ao
 * usuario e ao curso da requisicao; o adaptador so decide QUAL ferramenta chamar, nunca SOBRE QUEM
 * (RNF03). Implementacoes: {@code AgenteSpringAi} (Spring AI + Ollama) ou {@code AgenteDesabilitado}.
 *
 * @throws IaIndisponivelException se o LLM estiver fora, lento demais ou desabilitado
 */
public interface OrquestradorDeAgente {

    String conduzir(PedidoAoAgente pedido);

    /**
     * @param conversaId chave da memoria da conversa (um historico por usuario e curso)
     * @param instrucoes papel e regras do agente (mensagem de sistema)
     * @param mensagem   o que o usuario escreveu
     * @param ferramentas objetos com metodos anotados com {@code @Tool}
     */
    record PedidoAoAgente(String conversaId, String instrucoes, String mensagem, List<Object> ferramentas) {
    }
}
