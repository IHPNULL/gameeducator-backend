package org.facens.grupo_6_gameeducator.eventos.adaptador;

import org.facens.grupo_6_gameeducator.eventos.EmissorDeEventos;
import org.facens.grupo_6_gameeducator.eventos.EventoDeDominio;
import org.facens.grupo_6_gameeducator.exception.ServicoIndisponivelException;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

/**
 * ALTERNATIVA DESCARTADA, mantida so para o lab do Desafio 4 (app.eventos.modo=sincrono):
 * notifica via REST dentro da requisicao do aluno. Se o servico de notificacao estiver fora, a resposta
 * do aluno falha (503) e a transacao e revertida - exatamente o acoplamento que o RNF proibe.
 */
public class EmissorSincronoHttp implements EmissorDeEventos {

    private final RestClient restClient;
    private final JsonMapper json;

    public EmissorSincronoHttp(RestClient restClient, JsonMapper json) {
        this.restClient = restClient;
        this.json = json;
    }

    @Override
    public void emitir(EventoDeDominio evento) {
        try {
            restClient.post().uri("/notificacoes")
                    .header("X-Evento-Tipo", evento.tipo())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json.writeValueAsString(evento))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new ServicoIndisponivelException(
                    "Servico de notificacao indisponivel (modo sincrono): a operacao foi desfeita");
        }
    }
}
