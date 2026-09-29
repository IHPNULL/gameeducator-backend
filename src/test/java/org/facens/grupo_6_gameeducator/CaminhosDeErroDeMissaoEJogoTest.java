package org.facens.grupo_6_gameeducator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Desafio;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.exception.AcessoNegadoException;
import org.facens.grupo_6_gameeducator.exception.RecursoNaoEncontradoException;
import org.facens.grupo_6_gameeducator.exception.RegraDeNegocioException;
import org.facens.grupo_6_gameeducator.service.dto.NovaMissaoRequest;
import org.facens.grupo_6_gameeducator.service.dto.NovoDesafioRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Caminhos de erro de MissaoService e JogoService: validacoes e recursos inexistentes. */
@DisplayName("Caminhos de erro - missoes e jogo")
class CaminhosDeErroDeMissaoEJogoTest extends CenarioBase {

    private static final long ID_INEXISTENTE = 9_999L;

    private NovaMissaoRequest comDesafio(NovoDesafioRequest desafio) {
        return new NovaMissaoRequest("Missao 1", "descricao", List.of(desafio));
    }

    @Test
    @DisplayName("missao sem titulo e rejeitada (nulo e em branco)")
    void tituloObrigatorio() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        NovoDesafioRequest valido = new NovoDesafioRequest("Enunciado", List.of("a", "b"), 0, 10);

        assertThrows(RegraDeNegocioException.class, () -> missaoService.criar(professor.getId(), curso.getId(),
                new NovaMissaoRequest(null, "descricao", List.of(valido))));
        assertThrows(RegraDeNegocioException.class, () -> missaoService.criar(professor.getId(), curso.getId(),
                new NovaMissaoRequest("   ", "descricao", List.of(valido))));
    }

    @Test
    @DisplayName("missao com lista de desafios nula e rejeitada")
    void listaDeDesafiosNulaERejeitada() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);

        assertThrows(RegraDeNegocioException.class, () -> missaoService.criar(professor.getId(), curso.getId(),
                new NovaMissaoRequest("Missao 1", "descricao", null)));
    }

    @Test
    @DisplayName("desafio sem enunciado e rejeitado (nulo e em branco)")
    void enunciadoObrigatorio() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);

        assertThrows(RegraDeNegocioException.class, () -> missaoService.criar(professor.getId(), curso.getId(),
                comDesafio(new NovoDesafioRequest(null, List.of("a", "b"), 0, 10))));
        assertThrows(RegraDeNegocioException.class, () -> missaoService.criar(professor.getId(), curso.getId(),
                comDesafio(new NovoDesafioRequest("  ", List.of("a", "b"), 0, 10))));
    }

    @Test
    @DisplayName("desafio precisa de ao menos duas alternativas (nula e com uma so)")
    void alternativasInsuficientes() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);

        assertThrows(RegraDeNegocioException.class, () -> missaoService.criar(professor.getId(), curso.getId(),
                comDesafio(new NovoDesafioRequest("Enunciado", null, 0, 10))));
        assertThrows(RegraDeNegocioException.class, () -> missaoService.criar(professor.getId(), curso.getId(),
                comDesafio(new NovoDesafioRequest("Enunciado", List.of("unica"), 0, 10))));
    }

    @Test
    @DisplayName("gabarito fora do intervalo das alternativas e rejeitado")
    void gabaritoForaDoIntervalo() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);

        assertThrows(RegraDeNegocioException.class, () -> missaoService.criar(professor.getId(), curso.getId(),
                comDesafio(new NovoDesafioRequest("Enunciado", List.of("a", "b"), -1, 10))));
        assertThrows(RegraDeNegocioException.class, () -> missaoService.criar(professor.getId(), curso.getId(),
                comDesafio(new NovoDesafioRequest("Enunciado", List.of("a", "b"), 2, 10))));
    }

    @Test
    @DisplayName("XP negativo no desafio e rejeitado")
    void xpNegativoERejeitado() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);

        assertThrows(RegraDeNegocioException.class, () -> missaoService.criar(professor.getId(), curso.getId(),
                comDesafio(new NovoDesafioRequest("Enunciado", List.of("a", "b"), 0, -5))));
    }

    @Test
    @DisplayName("criar missao com usuario ou curso inexistente da 404")
    void criarComRecursoInexistente() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        NovaMissaoRequest request = missaoDeExemplo(10);

        assertThrows(RecursoNaoEncontradoException.class,
                () -> missaoService.criar(ID_INEXISTENTE, curso.getId(), request));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> missaoService.criar(professor.getId(), ID_INEXISTENTE, request));
    }

    @Test
    @DisplayName("listar missoes com usuario inexistente da 404")
    void listarComUsuarioInexistente() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);

        assertThrows(RecursoNaoEncontradoException.class,
                () -> missaoService.listarParaUsuario(ID_INEXISTENTE, curso.getId()));
    }

    @Test
    @DisplayName("professor sem vinculo nao lista as missoes do curso")
    void professorSemVinculoNaoLista() {
        Usuario responsavel = umProfessor("Ana");
        Usuario outroProfessor = umProfessor("Carlos");
        Curso curso = umCurso("Matematica Basica", responsavel);

        assertThrows(AcessoNegadoException.class,
                () -> missaoService.listarParaUsuario(outroProfessor.getId(), curso.getId()));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> missaoService.listarParaProfessor(responsavel.getId(), ID_INEXISTENTE));
    }

    @Test
    @DisplayName("professor nao responde desafio: quem joga e o aluno")
    void professorNaoRespondeDesafio() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        missaoService.criar(professor.getId(), curso.getId(), missaoDeExemplo(10));
        Desafio desafio = missaoRepository.findByCursoIdOrderByIdAsc(curso.getId()).get(0).getDesafios().get(0);

        assertThrows(AcessoNegadoException.class,
                () -> jogoService.responder(professor.getId(), desafio.getId(), 1));
    }

    @Test
    @DisplayName("alternativa fora do intervalo e rejeitada")
    void alternativaForaDoIntervalo() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);
        missaoService.criar(professor.getId(), curso.getId(), missaoDeExemplo(10));
        Desafio desafio = missaoRepository.findByCursoIdOrderByIdAsc(curso.getId()).get(0).getDesafios().get(0);

        assertThrows(RegraDeNegocioException.class,
                () -> jogoService.responder(aluno.getId(), desafio.getId(), -1));
        assertThrows(RegraDeNegocioException.class,
                () -> jogoService.responder(aluno.getId(), desafio.getId(), desafio.getAlternativas().size()));
        assertEquals(0, jogoService.xpNoCurso(aluno.getId(), curso.getId()),
                "tentativa invalida nao pode creditar XP");
    }

    @Test
    @DisplayName("responder com usuario ou desafio inexistente da 404")
    void responderComRecursoInexistente() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);
        missaoService.criar(professor.getId(), curso.getId(), missaoDeExemplo(10));
        Desafio desafio = missaoRepository.findByCursoIdOrderByIdAsc(curso.getId()).get(0).getDesafios().get(0);

        assertThrows(RecursoNaoEncontradoException.class,
                () -> jogoService.responder(ID_INEXISTENTE, desafio.getId(), 1));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> jogoService.responder(aluno.getId(), ID_INEXISTENTE, 1));
    }

    @Test
    @DisplayName("missao aceita descricao nula e desafios em lista mutavel")
    void missaoAceitaDescricaoNula() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);

        var missao = missaoService.criar(professor.getId(), curso.getId(), new NovaMissaoRequest(
                "Missao sem descricao", null,
                Arrays.asList(new NovoDesafioRequest("Enunciado", List.of("a", "b"), 0, 7))));

        assertEquals(7, missao.getXpTotal());
    }
}
