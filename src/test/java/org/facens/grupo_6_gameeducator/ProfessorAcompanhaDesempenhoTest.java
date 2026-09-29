package org.facens.grupo_6_gameeducator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Desafio;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.exception.AcessoNegadoException;
import org.facens.grupo_6_gameeducator.service.dto.DesempenhoAluno;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SCENARIO: Professor acompanha o desempenho da turma nas missoes de um curso.
 * (Product Backlog - "Acompanhar o desempenho da turma nas missoes", prioridade Importante)
 */
@DisplayName("Professor acompanha o desempenho da turma")
class ProfessorAcompanhaDesempenhoTest extends CenarioBase {

    private static final int INDICE_CORRETO = 1;
    private static final int INDICE_ERRADO = 0;

    @Test
    @DisplayName("mostra XP, tentativas e acertos de cada aluno matriculado")
    void mostraDesempenhoDosAlunosMatriculados() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        missaoService.criar(professor.getId(), curso.getId(), missaoDeExemplo(10));
        Desafio desafio = missaoRepository.findByCursoIdOrderByIdAsc(curso.getId()).get(0).getDesafios().get(0);

        Usuario bruno = umAluno("Bruno");
        matricular(bruno, curso);
        jogoService.responder(bruno.getId(), desafio.getId(), INDICE_ERRADO);
        jogoService.responder(bruno.getId(), desafio.getId(), INDICE_CORRETO);

        Usuario carla = umAluno("Carla");
        matricular(carla, curso);

        List<DesempenhoAluno> desempenho = jogoService.desempenhoDaTurma(professor.getId(), curso.getId());
        Map<Long, DesempenhoAluno> porAluno = desempenho.stream()
                .collect(java.util.stream.Collectors.toMap(d -> d.aluno().getId(), d -> d));

        DesempenhoAluno deBruno = porAluno.get(bruno.getId());
        assertEquals(10, deBruno.xpTotal());
        assertEquals(2, deBruno.tentativas());
        assertEquals(1, deBruno.acertos());

        DesempenhoAluno deCarla = porAluno.get(carla.getId());
        assertEquals(0, deCarla.xpTotal());
        assertEquals(0, deCarla.tentativas());
        assertEquals(0, deCarla.acertos());
    }

    @Test
    @DisplayName("403 quando quem consulta nao e o professor responsavel pelo curso")
    void negaAcessoParaProfessorQueNaoEResponsavel() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario outroProfessor = umProfessor("Carlos");

        assertThrows(AcessoNegadoException.class,
                () -> jogoService.desempenhoDaTurma(outroProfessor.getId(), curso.getId()));
    }
}
