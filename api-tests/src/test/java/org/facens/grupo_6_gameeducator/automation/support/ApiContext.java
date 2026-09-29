package org.facens.grupo_6_gameeducator.automation.support;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/**
 * Estado compartilhado entre os step definitions de um mesmo cenario: quem esta
 * "autenticado" (header X-Usuario-Id) e a ultima resposta HTTP recebida do backend.
 * Uma instancia nova e criada pelo Cucumber (via cucumber-picocontainer) a cada cenario.
 */
public class ApiContext {

    public static final String HEADER_USUARIO = "X-Usuario-Id";

    private final String baseUri = System.getProperty("automation.baseUrl", "http://localhost:8080");

    private Long usuarioAtualId;
    private Response ultimaResposta;

    public void autenticarComo(Long usuarioId) {
        this.usuarioAtualId = usuarioId;
    }

    public RequestSpecification requisicao() {
        RequestSpecification spec = given().baseUri(baseUri).contentType("application/json");
        if (usuarioAtualId != null) {
            spec = spec.header(HEADER_USUARIO, String.valueOf(usuarioAtualId));
        }
        return spec;
    }

    public void guardarResposta(Response resposta) {
        this.ultimaResposta = resposta;
    }

    public Response ultimaResposta() {
        if (ultimaResposta == null) {
            throw new IllegalStateException("Nenhuma requisicao foi feita ainda neste cenario");
        }
        return ultimaResposta;
    }
}
