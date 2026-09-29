package org.facens.grupo_6_gameeducator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Desafio;
import org.facens.grupo_6_gameeducator.domain.Missao;
import org.facens.grupo_6_gameeducator.domain.Tentativa;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.exception.AcessoNegadoException;
import org.facens.grupo_6_gameeducator.service.dto.NovaMissaoRequest;
import org.facens.grupo_6_gameeducator.service.dto.NovoDesafioRequest;
import org.facens.grupo_6_gameeducator.service.dto.ResultadoResposta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SCENARIO: Aluno resolve um desafio de uma missao e recebe XP.
 * (Sprint Backlog - US01 / Integrante 1)
 *
 * <p>Alternativa correta dos desafios de exemplo: indice 1.
 */
@DisplayName("Aluno resolve desafio e recebe XP")
class AlunoResolveDesafioTest extends CenarioBase {

    private static final int INDICE_CORRETO = 1;
    private static final int INDICE_ERRADO = 0;

    @Test
    @DisplayName("assert(respostaAvaliada) e assert(xpGanho === xpDoDesafio) e assert(xpTotal atualizado)")
    void acertarDesafioCreditaOXpDoDesafio() {
        Curso curso = cursoComMissaoPublicada(10);
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);
        Desafio desafio = primeiroDesafio(curso);

        int xpTotalAntes = jogoService.xpNoCurso(aluno.getId(), curso.getId());
        ResultadoResposta resultado = jogoService.responder(aluno.getId(), desafio.getId(), INDICE_CORRETO);
        int xpTotalDepois = jogoService.xpNoCurso(aluno.getId(), curso.getId());

        assertTrue(resultado.correta(), "o sistema deveria informar que a resposta esta correta");
        assertEquals(desafio.getXp(), resultado.xpGanho(), "o XP creditado deve ser o configurado no desafio");
        assertEquals(xpTotalAntes + resultado.xpGanho(), xpTotalDepois, "o XP total no curso deve ser atualizado");
        assertEquals(10, xpTotalDepois);
    }

    @Test
    @DisplayName("assert(xpGanho === 0) quando erra")
    void errarDesafioNaoCreditaXp() {
        Curso curso = cursoComMissaoPublicada(10);
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);
        Desafio desafio = primeiroDesafio(curso);

        int xpTotalAntes = jogoService.xpNoCurso(aluno.getId(), curso.getId());
        ResultadoResposta resultado = jogoService.responder(aluno.getId(), desafio.getId(), INDICE_ERRADO);
        int xpTotalDepois = jogoService.xpNoCurso(aluno.getId(), curso.getId());

        assertFalse(resultado.correta(), "o sistema deveria informar que a resposta esta incorreta");
        assertEquals(0, resultado.xpGanho(), "errar nao pode creditar XP");
        assertEquals(xpTotalAntes, xpTotalDepois, "o XP total nao pode mudar apos um erro");
    }

    @Test
    @DisplayName("XP acumula entre desafios diferentes da mesma missao")
    void xpAcumulaEntreDesafios() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Missao missao = missaoService.criar(professor.getId(), curso.getId(), new NovaMissaoRequest(
                "Missao 1 - Fracoes",
                "Dois desafios",
                List.of(
                        new NovoDesafioRequest("Desafio A", List.of("errada", "certa"), INDICE_CORRETO, 10),
                        new NovoDesafioRequest("Desafio B", List.of("errada", "certa"), INDICE_CORRETO, 15))));
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);

        jogoService.responder(aluno.getId(), missao.getDesafios().get(0).getId(), INDICE_CORRETO);
        ResultadoResposta segundo = jogoService.responder(aluno.getId(), missao.getDesafios().get(1).getId(), INDICE_CORRETO);

        assertEquals(15, segundo.xpGanho());
        assertEquals(25, segundo.xpTotalNoCurso(), "o XP dos dois desafios deve somar no curso");
    }

    @Test
    @DisplayName("acertar o mesmo desafio de novo nao acumula XP")
    void reacertarOMesmoDesafioNaoAcumulaXp() {
        Curso curso = cursoComMissaoPublicada(10);
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);
        Desafio desafio = primeiroDesafio(curso);

        jogoService.responder(aluno.getId(), desafio.getId(), INDICE_CORRETO);
        ResultadoResposta repeticao = jogoService.responder(aluno.getId(), desafio.getId(), INDICE_CORRETO);

        assertTrue(repeticao.correta());
        assertEquals(0, repeticao.xpGanho(), "o XP de um desafio e creditado uma unica vez");
        assertEquals(10, repeticao.xpTotalNoCurso());
    }

    @Test
    @DisplayName("aluno nao matriculado nao pode responder")
    void alunoNaoMatriculadoNaoPodeResponder() {
        Curso curso = cursoComMissaoPublicada(10);
        Usuario visitante = umAluno("Daniela");
        Desafio desafio = primeiroDesafio(curso);

        assertThrows(AcessoNegadoException.class,
                () -> jogoService.responder(visitante.getId(), desafio.getId(), INDICE_CORRETO));
        assertEquals(0, jogoService.xpNoCurso(visitante.getId(), curso.getId()));
    }

    @Test
    @DisplayName("cada resposta fica registrada no historico de tentativas")
    void tentativasFicamRegistradasNoHistorico() {
        Curso curso = cursoComMissaoPublicada(10);
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);
        Desafio desafio = primeiroDesafio(curso);

        jogoService.responder(aluno.getId(), desafio.getId(), INDICE_ERRADO);
        jogoService.responder(aluno.getId(), desafio.getId(), INDICE_CORRETO);

        List<Tentativa> historico = jogoService.historico(aluno.getId(), desafio.getId());
        assertEquals(2, historico.size(), "as duas tentativas deveriam estar no historico");
        assertEquals(1, historico.stream().filter(Tentativa::isCorreta).count());
    }

    private Curso cursoComMissaoPublicada(int xp) {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        missaoService.criar(professor.getId(), curso.getId(), missaoDeExemplo(xp));
        return curso;
    }

    private Desafio primeiroDesafio(Curso curso) {
        return missaoRepository.findByCursoIdOrderByIdAsc(curso.getId()).get(0).getDesafios().get(0);
    }
}
