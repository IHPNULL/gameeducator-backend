package org.facens.grupo_6_gameeducator.ia;

/**
 * Porta de saida para o servico de IA (FastAPI). Toda falha de comunicacao chega aos services
 * como {@link IaIndisponivelException}: timeout, conexao recusada, 5xx ou disjuntor aberto.
 */
public interface ServicoDeIa {

    RecomendacoesDaIa recomendar(PedidoDeRecomendacao pedido);

    RespostaDoAssistente perguntar(PerguntaAoAssistente pergunta);
}
