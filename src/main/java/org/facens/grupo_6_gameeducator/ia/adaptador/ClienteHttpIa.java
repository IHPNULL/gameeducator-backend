package org.facens.grupo_6_gameeducator.ia.adaptador;

import org.facens.grupo_6_gameeducator.ia.IaIndisponivelException;
import org.facens.grupo_6_gameeducator.ia.PedidoDeRecomendacao;
import org.facens.grupo_6_gameeducator.ia.PerguntaAoAssistente;
import org.facens.grupo_6_gameeducator.ia.RecomendacoesDaIa;
import org.facens.grupo_6_gameeducator.ia.RespostaDoAssistente;
import org.facens.grupo_6_gameeducator.ia.ServicoDeIa;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Adaptador HTTP/JSON para o FastAPI. Os timeouts vem do RestClient montado em IaConfig. */
public class ClienteHttpIa implements ServicoDeIa {

    private final RestClient restClient;

    public ClienteHttpIa(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public RecomendacoesDaIa recomendar(PedidoDeRecomendacao pedido) {
        return post("/recomendacoes", pedido, RecomendacoesDaIa.class);
    }

    @Override
    public RespostaDoAssistente perguntar(PerguntaAoAssistente pergunta) {
        return post("/assistente/perguntas", pergunta, RespostaDoAssistente.class);
    }

    private <T> T post(String caminho, Object corpo, Class<T> tipo) {
        try {
            T resposta = restClient.post().uri(caminho).body(corpo).retrieve().body(tipo);
            if (resposta == null) {
                throw new IaIndisponivelException("Servico de IA respondeu sem corpo em " + caminho, null);
            }
            return resposta;
        } catch (RestClientException e) {
            throw new IaIndisponivelException("Falha ao chamar o servico de IA em " + caminho + ": " + e.getMessage(), e);
        }
    }
}
