package org.facens.grupo_6_gameeducator.automation.steps;

import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import org.facens.grupo_6_gameeducator.automation.support.ApiContext;
import org.facens.grupo_6_gameeducator.automation.support.Fixture;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class NotasApiSteps {

    private final ApiContext ctx;

    public NotasApiSteps(ApiContext ctx) {
        this.ctx = ctx;
    }

    // "{double}" usa DecimalFormat sensivel a locale (ex.: trata "." como separador de milhar),
    // entao "9.0" pode virar 90.0 dependendo do ambiente. "{word}" + Double.parseDouble evita isso.
    @Quando("lanco a nota {string} com valor {word} para o primeiro aluno")
    public void lancoUmaNota(String avaliacao, String valorTexto) {
        double valor = Double.parseDouble(valorTexto);
        ctx.guardarResposta(ctx.requisicao()
                .body(Map.of("avaliacao", avaliacao, "valor", valor))
                .post("/api/cursos/{cursoId}/notas/{alunoId}", Fixture.CURSO_ID, Fixture.ALUNO_UM_ID));
    }

    @Quando("lanco uma nota para o primeiro aluno em um curso inexistente")
    public void lancoNotaEmCursoInexistente() {
        ctx.guardarResposta(ctx.requisicao()
                .body(Map.of("avaliacao", "Prova X", "valor", 8.0))
                .post("/api/cursos/{cursoId}/notas/{alunoId}", Fixture.CURSO_INEXISTENTE, Fixture.ALUNO_UM_ID));
    }

    @Quando("consulto minhas notas no curso de automacao")
    public void consultoMinhasNotas() {
        ctx.guardarResposta(ctx.requisicao().get("/api/cursos/{cursoId}/notas", Fixture.CURSO_ID));
    }

    @Entao("a nota lancada tem o valor {word}")
    public void aNotaLancadaTemOValor(String valorEsperadoTexto) {
        double valorEsperado = Double.parseDouble(valorEsperadoTexto);
        assertThat(ctx.ultimaResposta().jsonPath().getDouble("valor")).isEqualTo(valorEsperado);
    }

    @Entao("a lista de notas tem ao menos {int} item")
    public void aListaDeNotasTemAoMenosItens(int minimo) {
        assertThat(ctx.ultimaResposta().jsonPath().getList("$")).hasSizeGreaterThanOrEqualTo(minimo);
    }
}
