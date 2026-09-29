package org.facens.grupo_6_gameeducator.automation.steps;

import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import org.facens.grupo_6_gameeducator.automation.support.ApiContext;
import org.facens.grupo_6_gameeducator.automation.support.Fixture;

import static org.assertj.core.api.Assertions.assertThat;

public class TurmaApiSteps {

    private final ApiContext ctx;

    public TurmaApiSteps(ApiContext ctx) {
        this.ctx = ctx;
    }

    @Quando("consulto o desempenho da turma do curso de automacao")
    public void consultoODesempenhoDaTurma() {
        ctx.guardarResposta(ctx.requisicao().get("/api/cursos/{cursoId}/turma/desempenho", Fixture.CURSO_ID));
    }

    @Quando("consulto o desempenho da turma de um curso inexistente")
    public void consultoODesempenhoDeCursoInexistente() {
        ctx.guardarResposta(ctx.requisicao().get("/api/cursos/{cursoId}/turma/desempenho", Fixture.CURSO_INEXISTENTE));
    }

    @Entao("a lista de desempenho tem ao menos {int} itens")
    public void aListaDeDesempenhoTemAoMenosItens(int minimo) {
        assertThat(ctx.ultimaResposta().jsonPath().getList("$")).hasSizeGreaterThanOrEqualTo(minimo);
    }
}
