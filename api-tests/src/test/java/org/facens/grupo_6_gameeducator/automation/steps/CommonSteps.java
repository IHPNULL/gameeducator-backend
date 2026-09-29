package org.facens.grupo_6_gameeducator.automation.steps;

import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import org.facens.grupo_6_gameeducator.automation.support.ApiContext;
import org.facens.grupo_6_gameeducator.automation.support.Fixture;

import static org.assertj.core.api.Assertions.assertThat;

/** Passos reaproveitados por todas as features: quem esta autenticado e o status da ultima resposta. */
public class CommonSteps {

    private final ApiContext ctx;

    public CommonSteps(ApiContext ctx) {
        this.ctx = ctx;
    }

    @Dado("que estou autenticado como o professor responsavel pelo curso de automacao")
    public void autenticadoComoProfessor() {
        ctx.autenticarComo(Fixture.PROFESSOR_ID);
    }

    @Dado("que estou autenticado como um aluno matriculado no curso de automacao")
    public void autenticadoComoAlunoUm() {
        ctx.autenticarComo(Fixture.ALUNO_UM_ID);
    }

    @Dado("que estou autenticado como o segundo aluno matriculado no curso de automacao")
    public void autenticadoComoAlunoDois() {
        ctx.autenticarComo(Fixture.ALUNO_DOIS_ID);
    }

    @Dado("que estou autenticado como um usuario inexistente")
    public void autenticadoComoUsuarioInexistente() {
        ctx.autenticarComo(Fixture.ID_INEXISTENTE);
    }

    @Entao("a resposta tem status {int}")
    public void respostaTemStatus(int statusEsperado) {
        assertThat(ctx.ultimaResposta().statusCode()).isEqualTo(statusEsperado);
    }

    @Entao("a mensagem de erro contem {string}")
    public void mensagemDeErroContem(String trecho) {
        assertThat(ctx.ultimaResposta().jsonPath().getString("mensagem")).containsIgnoringCase(trecho);
    }
}
