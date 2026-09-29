package org.facens.grupo_6_gameeducator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Post;
import org.facens.grupo_6_gameeducator.domain.RespostaPost;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.exception.AcessoNegadoException;
import org.facens.grupo_6_gameeducator.exception.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * BDD: Dado que estou autenticado como aluno E estou matriculado em um curso,
 * quando clico no curso E clico na aba de foruns, entao vejo os posts,
 * posso responder e ser respondido E posso publicar meu proprio post.
 */
@DisplayName("Aluno interage com o forum de um curso")
class AlunoInterageNoForumTest extends CenarioBase {

    @Test
    @DisplayName("assert(postsExibidos === true) - vejo os posts do forum")
    void deveExibirOsPostsDoForumDoCurso() {
        // Dado que estou autenticado como aluno E estou matriculado em um curso
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario bruno = umAluno("Bruno");
        Usuario daniela = umAluno("Daniela");
        matricular(bruno, curso);
        matricular(daniela, curso);

        // E ja existem posts publicados no forum
        forumService.publicar(daniela.getId(), curso.getId(), "Duvida na fracao", "Alguem me explica 1/2 + 1/4?");
        forumService.publicar(professor.getId(), curso.getId(), "Aviso", "Prova na proxima semana");

        // Quando clico no curso E clico na aba de foruns
        List<Post> posts = forumService.listarPosts(bruno.getId(), curso.getId());

        // Entao vejo os posts
        assertEquals(2, posts.size());
        assertTrue(posts.stream().anyMatch(p -> p.getTitulo().equals("Duvida na fracao")));
        assertTrue(posts.stream().anyMatch(p -> p.getTitulo().equals("Aviso")));
    }

    @Test
    @DisplayName("assert(respostaPublicada === true) - posso responder um post")
    void devoConseguirResponderUmPost() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario bruno = umAluno("Bruno");
        Usuario daniela = umAluno("Daniela");
        matricular(bruno, curso);
        matricular(daniela, curso);

        Post postDaDaniela = forumService.publicar(
                daniela.getId(), curso.getId(), "Duvida na fracao", "Alguem me explica 1/2 + 1/4?");

        RespostaPost resposta = forumService.responder(
                bruno.getId(), postDaDaniela.getId(), "E 3/4: iguala os denominadores primeiro.");

        assertTrue(resposta.getId() != null, "a resposta deveria ter sido registrada");
        List<RespostaPost> respostas = forumService.respostasDo(postDaDaniela.getId());
        assertEquals(1, respostas.size());
        assertEquals("E 3/4: iguala os denominadores primeiro.", respostas.get(0).getConteudo());
        assertEquals(bruno.getId(), respostas.get(0).getAutor().getId());
    }

    @Test
    @DisplayName("posso ser respondido - outro aluno responde o meu post")
    void devoConseguirSerRespondido() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario bruno = umAluno("Bruno");
        Usuario daniela = umAluno("Daniela");
        matricular(bruno, curso);
        matricular(daniela, curso);

        Post meuPost = forumService.publicar(
                bruno.getId(), curso.getId(), "Como simplificar 4/8?", "Travei nessa questao");

        forumService.responder(daniela.getId(), meuPost.getId(), "Divide os dois por 4: da 1/2.");
        forumService.responder(professor.getId(), meuPost.getId(), "Isso mesmo, Daniela.");

        List<RespostaPost> respostas = forumService.respostasDo(meuPost.getId());
        assertEquals(2, respostas.size(), "o meu post deveria ter as duas respostas");
        assertEquals(daniela.getId(), respostas.get(0).getAutor().getId());
        assertEquals(professor.getId(), respostas.get(1).getAutor().getId());
    }

    @Test
    @DisplayName("assert(postCriado === true) - posso publicar meu proprio post")
    void devoConseguirPublicarMeuProprioPost() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario bruno = umAluno("Bruno");
        matricular(bruno, curso);

        Post publicado = forumService.publicar(
                bruno.getId(), curso.getId(), "Como simplificar 4/8?", "Travei nessa questao");

        assertTrue(publicado.getId() != null, "o post deveria ter sido persistido");
        assertEquals(bruno.getId(), publicado.getAutor().getId());

        List<Post> posts = forumService.listarPosts(bruno.getId(), curso.getId());
        assertTrue(posts.stream().anyMatch(p -> p.getId().equals(publicado.getId())),
                "o post publicado deveria aparecer no forum");
    }

    @Test
    @DisplayName("o forum exibido e apenas o daquele curso")
    void naoDeveMisturarPostsDeOutroCurso() {
        Usuario professor = umProfessor("Ana");
        Curso matematica = umCurso("Matematica Basica", professor);
        Curso portugues = umCurso("Portugues", professor);
        Usuario bruno = umAluno("Bruno");
        matricular(bruno, matematica);
        matricular(bruno, portugues);

        forumService.publicar(bruno.getId(), matematica.getId(), "Duvida de fracao", "...");
        forumService.publicar(bruno.getId(), portugues.getId(), "Duvida de crase", "...");

        List<Post> forumDeMatematica = forumService.listarPosts(bruno.getId(), matematica.getId());

        assertEquals(1, forumDeMatematica.size());
        assertEquals("Duvida de fracao", forumDeMatematica.get(0).getTitulo());
    }

    @Test
    @DisplayName("aluno sem matricula nao acessa nem publica no forum")
    void alunoNaoMatriculadoNaoAcessaOForum() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario matriculado = umAluno("Bruno");
        Usuario visitante = umAluno("Daniela");
        matricular(matriculado, curso);
        Post post = forumService.publicar(matriculado.getId(), curso.getId(), "Duvida", "...");

        assertThrows(AcessoNegadoException.class,
                () -> forumService.listarPosts(visitante.getId(), curso.getId()));
        assertThrows(AcessoNegadoException.class,
                () -> forumService.publicar(visitante.getId(), curso.getId(), "Oi", "..."));
        assertThrows(AcessoNegadoException.class,
                () -> forumService.responder(visitante.getId(), post.getId(), "..."));
    }

    @Test
    @DisplayName("post e resposta sem conteudo sao rejeitados")
    void conteudoVazioERejeitado() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario bruno = umAluno("Bruno");
        matricular(bruno, curso);
        Post post = forumService.publicar(bruno.getId(), curso.getId(), "Duvida", "conteudo");

        assertThrows(RegraDeNegocioException.class,
                () -> forumService.publicar(bruno.getId(), curso.getId(), "  ", "conteudo"));
        assertThrows(RegraDeNegocioException.class,
                () -> forumService.publicar(bruno.getId(), curso.getId(), "Titulo", "  "));
        assertThrows(RegraDeNegocioException.class,
                () -> forumService.responder(bruno.getId(), post.getId(), "  "));
    }
}
