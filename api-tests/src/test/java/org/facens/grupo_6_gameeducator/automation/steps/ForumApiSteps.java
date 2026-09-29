package org.facens.grupo_6_gameeducator.automation.steps;

import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import org.facens.grupo_6_gameeducator.automation.support.ApiContext;
import org.facens.grupo_6_gameeducator.automation.support.Fixture;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ForumApiSteps {

    private final ApiContext ctx;

    public ForumApiSteps(ApiContext ctx) {
        this.ctx = ctx;
    }

    @Quando("listo os posts do forum do curso de automacao")
    public void listoOsPosts() {
        ctx.guardarResposta(ctx.requisicao().get("/api/cursos/{cursoId}/forum", Fixture.CURSO_ID));
    }

    @Quando("publico o post {string} com conteudo {string}")
    public void publicoUmPost(String titulo, String conteudo) {
        ctx.guardarResposta(ctx.requisicao()
                .body(Map.of("titulo", titulo, "conteudo", conteudo))
                .post("/api/cursos/{cursoId}/forum", Fixture.CURSO_ID));
    }

    @Quando("publico um post sem titulo")
    public void publicoUmPostSemTitulo() {
        ctx.guardarResposta(ctx.requisicao()
                .body(Map.of("titulo", "", "conteudo", "Conteudo sem titulo"))
                .post("/api/cursos/{cursoId}/forum", Fixture.CURSO_ID));
    }

    @Quando("respondo o post inicial com o conteudo {string}")
    public void respondoOPostInicial(String conteudo) {
        ctx.guardarResposta(ctx.requisicao()
                .body(Map.of("conteudo", conteudo))
                .post("/api/forum/posts/{postId}/respostas", Fixture.POST_INICIAL_ID));
    }

    @Quando("respondo um post inexistente")
    public void respondoUmPostInexistente() {
        ctx.guardarResposta(ctx.requisicao()
                .body(Map.of("conteudo", "Resposta qualquer"))
                .post("/api/forum/posts/{postId}/respostas", Fixture.ID_INEXISTENTE));
    }

    @Entao("a lista de posts tem ao menos {int} item")
    public void aListaDePostsTemAoMenosItens(int minimo) {
        assertThat(ctx.ultimaResposta().jsonPath().getList("$")).hasSizeGreaterThanOrEqualTo(minimo);
    }

    @Entao("o post publicado tem o titulo {string}")
    public void oPostPublicadoTemOTitulo(String tituloEsperado) {
        assertThat(ctx.ultimaResposta().jsonPath().getString("titulo")).isEqualTo(tituloEsperado);
    }
}
