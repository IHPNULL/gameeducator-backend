package org.facens.grupo_6_gameeducator.automation.steps;

import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import org.facens.grupo_6_gameeducator.automation.support.ApiContext;
import org.facens.grupo_6_gameeducator.automation.support.Fixture;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class MissaoApiSteps {

    private final ApiContext ctx;

    public MissaoApiSteps(ApiContext ctx) {
        this.ctx = ctx;
    }

    @Quando("crio uma missao valida chamada {string}")
    public void crioUmaMissaoValida(String titulo) {
        Map<String, Object> desafio = Map.of(
                "enunciado", "Quanto e 1 + 1 ?",
                "alternativas", List.of("1", "2", "3"),
                "indiceRespostaCorreta", 1,
                "xp", 10);
        Map<String, Object> body = Map.of(
                "titulo", titulo,
                "descricao", "Missao criada pela suite de automacao",
                "desafios", List.of(desafio));

        ctx.guardarResposta(ctx.requisicao().body(body)
                .post("/api/cursos/{cursoId}/missoes", Fixture.CURSO_ID));
    }

    @Quando("tento criar uma missao sem titulo")
    public void tentoCriarMissaoSemTitulo() {
        Map<String, Object> desafio = Map.of(
                "enunciado", "Enunciado qualquer",
                "alternativas", List.of("a", "b"),
                "indiceRespostaCorreta", 0,
                "xp", 10);
        Map<String, Object> body = Map.of(
                "titulo", "",
                "descricao", "Falta o titulo",
                "desafios", List.of(desafio));

        ctx.guardarResposta(ctx.requisicao().body(body)
                .post("/api/cursos/{cursoId}/missoes", Fixture.CURSO_ID));
    }

    @Quando("listo as missoes do curso de automacao")
    public void listoAsMissoesDoCursoDeAutomacao() {
        ctx.guardarResposta(ctx.requisicao().get("/api/cursos/{cursoId}/missoes", Fixture.CURSO_ID));
    }

    @Quando("listo as missoes de um curso inexistente")
    public void listoAsMissoesDeUmCursoInexistente() {
        ctx.guardarResposta(ctx.requisicao().get("/api/cursos/{cursoId}/missoes", Fixture.CURSO_INEXISTENTE));
    }

    @Entao("a missao criada tem o titulo {string}")
    public void aMissaoCriadaTemOTitulo(String tituloEsperado) {
        assertThat(ctx.ultimaResposta().jsonPath().getString("titulo")).isEqualTo(tituloEsperado);
    }

    @Entao("a lista de missoes tem ao menos {int} item")
    public void aListaDeMissoesTemAoMenosItens(int minimo) {
        assertThat(ctx.ultimaResposta().jsonPath().getList("$")).hasSizeGreaterThanOrEqualTo(minimo);
    }
}
