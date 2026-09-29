package org.facens.grupo_6_gameeducator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Desafio;
import org.facens.grupo_6_gameeducator.domain.Medalha;
import org.facens.grupo_6_gameeducator.domain.Missao;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.service.dto.NovaMissaoRequest;
import org.facens.grupo_6_gameeducator.service.dto.NovoDesafioRequest;
import org.facens.grupo_6_gameeducator.service.dto.ResultadoResposta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SCENARIO: Aluno ganha uma medalha ao atingir um marco de XP no curso.
 *
 * <p>Marcos de XP concedem medalha (50, 100, 250, 500). Uma medalha e concedida uma unica vez
 * por marco: cruzar o mesmo marco de novo (ou nao cruzar nenhum) nao gera outra.
 */
@DisplayName("Aluno ganha medalha ao atingir marco de XP")
class AlunoGanhaMedalhaTest extends CenarioBase {

    private static final int INDICE_CORRETO = 1;

    @Test
    @DisplayName("ao cruzar o marco de 50 XP, o aluno ganha a medalha")
    void acertarDesafioDe50XpConcedeMedalha() {
        Curso curso = cursoComDesafioDeXp(50);
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);
        Desafio desafio = primeiroDesafio(curso);

        ResultadoResposta resultado = jogoService.responder(aluno.getId(), desafio.getId(), INDICE_CORRETO);

        assertEquals(List.of(50), resultado.medalhasConquistadas());
        List<Medalha> medalhas = jogoService.medalhasDoAluno(aluno.getId(), curso.getId());
        assertEquals(1, medalhas.size());
        assertEquals(50, medalhas.get(0).getMarcoXp());
    }

    @Test
    @DisplayName("reacertar o mesmo desafio nao concede a medalha de novo")
    void reacertarNaoConcedeMedalhaDuplicada() {
        Curso curso = cursoComDesafioDeXp(50);
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);
        Desafio desafio = primeiroDesafio(curso);

        jogoService.responder(aluno.getId(), desafio.getId(), INDICE_CORRETO);
        ResultadoResposta repeticao = jogoService.responder(aluno.getId(), desafio.getId(), INDICE_CORRETO);

        assertTrue(repeticao.medalhasConquistadas().isEmpty());
        assertEquals(1, jogoService.medalhasDoAluno(aluno.getId(), curso.getId()).size());
    }

    @Test
    @DisplayName("ao cruzar dois marcos na mesma resposta, ambas as medalhas sao concedidas")
    void cruzarDoisMarcosDeUmaVezConcedeAsDuasMedalhas() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Missao missao = missaoService.criar(professor.getId(), curso.getId(), new NovaMissaoRequest(
                "Missao 1", "um desafio grande",
                List.of(new NovoDesafioRequest("Desafio", List.of("errada", "certa"), INDICE_CORRETO, 120))));
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);

        ResultadoResposta resultado = jogoService.responder(
                aluno.getId(), missao.getDesafios().get(0).getId(), INDICE_CORRETO);

        assertEquals(List.of(50, 100), resultado.medalhasConquistadas());
    }

    @Test
    @DisplayName("marco ja concedido nao e re-emitido ao cruzar um marco maior depois")
    void marcoAnteriorNaoEReemitido() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Missao missao = missaoService.criar(professor.getId(), curso.getId(), new NovaMissaoRequest(
                "Missao 1", "dois desafios",
                List.of(
                        new NovoDesafioRequest("Desafio A", List.of("errada", "certa"), INDICE_CORRETO, 50),
                        new NovoDesafioRequest("Desafio B", List.of("errada", "certa"), INDICE_CORRETO, 50))));
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);

        jogoService.responder(aluno.getId(), missao.getDesafios().get(0).getId(), INDICE_CORRETO);
        ResultadoResposta segundo = jogoService.responder(
                aluno.getId(), missao.getDesafios().get(1).getId(), INDICE_CORRETO);

        assertEquals(List.of(100), segundo.medalhasConquistadas());
        assertEquals(2, jogoService.medalhasDoAluno(aluno.getId(), curso.getId()).size());
    }

    private Curso cursoComDesafioDeXp(int xp) {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        missaoService.criar(professor.getId(), curso.getId(), missaoDeExemplo(xp));
        return curso;
    }

    private Desafio primeiroDesafio(Curso curso) {
        return missaoRepository.findByCursoIdOrderByIdAsc(curso.getId()).get(0).getDesafios().get(0);
    }
}
