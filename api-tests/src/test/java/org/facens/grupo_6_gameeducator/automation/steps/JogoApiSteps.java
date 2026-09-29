package org.facens.grupo_6_gameeducator.automation.steps;

import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import org.facens.grupo_6_gameeducator.automation.support.ApiContext;
import org.facens.grupo_6_gameeducator.automation.support.Fixture;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class JogoApiSteps {

    private final ApiContext ctx;

    public JogoApiSteps(ApiContext ctx) {
        this.ctx = ctx;
    }

    @Quando("respondo o desafio de soma com a alternativa correta")
    public void respondoDesafioSomaComAlternativaCorreta() {
        responder(Fixture.DESAFIO_SOMA_ID, 1);
    }

    @Quando("respondo o desafio da capital com uma alternativa errada")
    public void respondoDesafioCapitalComAlternativaErrada() {
        responder(Fixture.DESAFIO_CAPITAL_ID, 0);
    }

    @Quando("respondo o desafio de soma com o indice de alternativa {int}")
    public void respondoDesafioSomaComIndiceInvalido(int indice) {
        responder(Fixture.DESAFIO_SOMA_ID, indice);
    }

    @Quando("respondo um desafio inexistente com a alternativa correta")
    public void respondoDesafioInexistente() {
        responder(Fixture.ID_INEXISTENTE, 1);
    }

    private void responder(long desafioId, int indiceResposta) {
        ctx.guardarResposta(ctx.requisicao()
                .body(Map.of("indiceResposta", indiceResposta))
                .post("/api/desafios/{desafioId}/respostas", desafioId));
    }

    @Quando("consulto o ranking do curso de automacao")
    public void consultoORanking() {
        ctx.guardarResposta(ctx.requisicao().get("/api/cursos/{cursoId}/ranking", Fixture.CURSO_ID));
    }

    @Quando("consulto meu progresso no curso de automacao")
    public void consultoMeuProgresso() {
        ctx.guardarResposta(ctx.requisicao().get("/api/cursos/{cursoId}/progresso", Fixture.CURSO_ID));
    }

    @Entao("a resposta indica que a alternativa esta correta")
    public void aRespostaIndicaQueEstaCorreta() {
        assertThat(ctx.ultimaResposta().jsonPath().getBoolean("correta")).isTrue();
    }

    @Entao("a resposta indica que a alternativa esta incorreta")
    public void aRespostaIndicaQueEstaIncorreta() {
        assertThat(ctx.ultimaResposta().jsonPath().getBoolean("correta")).isFalse();
    }

    @Entao("a lista de ranking tem ao menos {int} item")
    public void aListaDeRankingTemAoMenosItens(int minimo) {
        assertThat(ctx.ultimaResposta().jsonPath().getList("$")).hasSizeGreaterThanOrEqualTo(minimo);
    }
}
