package org.facens.grupo_6_gameeducator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Post;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.exception.AcessoNegadoException;
import org.facens.grupo_6_gameeducator.exception.RecursoNaoEncontradoException;
import org.facens.grupo_6_gameeducator.exception.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Caminhos de erro de NotaService e ForumService. */
@DisplayName("Caminhos de erro - notas e forum")
class CaminhosDeErroDeNotasEForumTest extends CenarioBase {

    private static final long ID_INEXISTENTE = 9_999L;

    @Test
    @DisplayName("aluno nao lanca nota")
    void alunoNaoLancaNota() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario bruno = umAluno("Bruno");
        Usuario daniela = umAluno("Daniela");
        matricular(bruno, curso);
        matricular(daniela, curso);

        assertThrows(AcessoNegadoException.class, () -> notaService.lancar(
                bruno.getId(), curso.getId(), daniela.getId(), "Prova 1", new BigDecimal("10.00")));
    }

    @Test
    @DisplayName("avaliacao sem nome e rejeitada (nula e em branco)")
    void avaliacaoObrigatoria() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);

        assertThrows(RegraDeNegocioException.class, () -> notaService.lancar(
                professor.getId(), curso.getId(), aluno.getId(), null, new BigDecimal("8.00")));
        assertThrows(RegraDeNegocioException.class, () -> notaService.lancar(
                professor.getId(), curso.getId(), aluno.getId(), "   ", new BigDecimal("8.00")));
    }

    @Test
    @DisplayName("nota nula ou negativa e rejeitada")
    void valorDaNotaEValidado() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);

        assertThrows(RegraDeNegocioException.class, () -> notaService.lancar(
                professor.getId(), curso.getId(), aluno.getId(), "Prova 1", null));
        assertThrows(RegraDeNegocioException.class, () -> notaService.lancar(
                professor.getId(), curso.getId(), aluno.getId(), "Prova 1", new BigDecimal("-0.50")));
        assertEquals(0, notaService.minhasNotas(aluno.getId(), curso.getId()).size());
    }

    @Test
    @DisplayName("nota nos extremos 0 e 10 e aceita")
    void extremosSaoAceitos() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);

        notaService.lancar(professor.getId(), curso.getId(), aluno.getId(), "Zero", BigDecimal.ZERO);
        notaService.lancar(professor.getId(), curso.getId(), aluno.getId(), "Dez", BigDecimal.TEN);

        assertEquals(2, notaService.minhasNotas(aluno.getId(), curso.getId()).size());
    }

    @Test
    @DisplayName("lancar nota com usuario, curso ou aluno inexistente da 404")
    void lancarComRecursoInexistente() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);
        BigDecimal nota = new BigDecimal("8.00");

        assertThrows(RecursoNaoEncontradoException.class, () -> notaService.lancar(
                ID_INEXISTENTE, curso.getId(), aluno.getId(), "Prova 1", nota));
        assertThrows(RecursoNaoEncontradoException.class, () -> notaService.lancar(
                professor.getId(), ID_INEXISTENTE, aluno.getId(), "Prova 1", nota));
        assertThrows(RecursoNaoEncontradoException.class, () -> notaService.lancar(
                professor.getId(), curso.getId(), ID_INEXISTENTE, "Prova 1", nota));
    }

    @Test
    @DisplayName("professor responsavel acessa o forum do seu curso sem estar matriculado")
    void professorAcessaForumDoSeuCurso() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);

        Post post = forumService.publicar(professor.getId(), curso.getId(), "Aviso", "Prova semana que vem");

        assertEquals(1, forumService.listarPosts(professor.getId(), curso.getId()).size());
        assertEquals(professor.getId(), post.getAutor().getId());
    }

    @Test
    @DisplayName("post com titulo ou conteudo nulo e rejeitado")
    void postComCamposNulos() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);

        assertThrows(RegraDeNegocioException.class,
                () -> forumService.publicar(aluno.getId(), curso.getId(), null, "conteudo"));
        assertThrows(RegraDeNegocioException.class,
                () -> forumService.publicar(aluno.getId(), curso.getId(), "Titulo", null));
    }

    @Test
    @DisplayName("resposta com conteudo nulo e rejeitada")
    void respostaComConteudoNulo() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);
        Post post = forumService.publicar(aluno.getId(), curso.getId(), "Duvida", "conteudo");

        assertThrows(RegraDeNegocioException.class,
                () -> forumService.responder(aluno.getId(), post.getId(), null));
    }

    @Test
    @DisplayName("forum com usuario, curso ou post inexistente da 404")
    void forumComRecursoInexistente() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);

        assertThrows(RecursoNaoEncontradoException.class,
                () -> forumService.listarPosts(ID_INEXISTENTE, curso.getId()));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> forumService.listarPosts(aluno.getId(), ID_INEXISTENTE));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> forumService.responder(aluno.getId(), ID_INEXISTENTE, "resposta"));
    }
}
