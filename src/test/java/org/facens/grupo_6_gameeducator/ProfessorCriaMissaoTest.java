package org.facens.grupo_6_gameeducator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Desafio;
import org.facens.grupo_6_gameeducator.domain.Missao;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.exception.AcessoNegadoException;
import org.facens.grupo_6_gameeducator.exception.RegraDeNegocioException;
import org.facens.grupo_6_gameeducator.service.dto.NovaMissaoRequest;
import org.facens.grupo_6_gameeducator.service.dto.NovoDesafioRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SCENARIO: Professor cria uma missao com desafios em um curso sob sua responsabilidade.
 * (Sprint Backlog - US02 / Integrante 2)
 */
@DisplayName("Professor cria missao com desafios")
class ProfessorCriaMissaoTest extends CenarioBase {

    @Test
    @DisplayName("assert(missaoCriada) e assert(desafiosSalvos) e assert(xpDoDesafio)")
    void deveCriarMissaoComOsDesafiosInformados() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);

        NovaMissaoRequest request = new NovaMissaoRequest(
                "Missao 1 - Fracoes",
                "Resolva os desafios sobre fracoes equivalentes",
                List.of(
                        new NovoDesafioRequest("Quanto e 1/2 + 1/4 ?", List.of("1/6", "3/4", "2/6", "1/8"), 1, 10),
                        new NovoDesafioRequest("1/3 e equivalente a ?", List.of("2/6", "3/4"), 0, 15)));

        Missao missao = missaoService.criar(professor.getId(), curso.getId(), request);

        // assert(missaoCriada === true) - a missao e persistida e aparece na listagem do curso
        assertTrue(missao.getId() != null, "a missao deveria ter sido persistida");
        List<Missao> missoesDoCurso = missaoService.listarParaProfessor(professor.getId(), curso.getId());
        assertEquals(1, missoesDoCurso.size());
        assertEquals("Missao 1 - Fracoes", missoesDoCurso.get(0).getTitulo());

        // assert(desafiosSalvos === desafiosInformados) - todos os desafios enviados foram gravados
        List<Desafio> desafios = missao.getDesafios();
        assertEquals(2, desafios.size());
        assertEquals("Quanto e 1/2 + 1/4 ?", desafios.get(0).getEnunciado());
        assertEquals(List.of("1/6", "3/4", "2/6", "1/8"), desafios.get(0).getAlternativas());

        // assert(xpDoDesafio === xpInformado) - o XP de cada desafio e o informado pelo professor
        assertEquals(10, desafios.get(0).getXp());
        assertEquals(15, desafios.get(1).getXp());
        assertEquals(25, missao.getXpTotal());
    }

    @Test
    @DisplayName("assert(missaoVisivelParaAluno)")
    void missaoDeveFicarVisivelParaAlunoMatriculado() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);

        missaoService.criar(professor.getId(), curso.getId(), missaoDeExemplo(10));

        List<Missao> visiveis = missaoService.listarParaAluno(aluno.getId(), curso.getId());
        assertEquals(1, visiveis.size(), "aluno matriculado deveria enxergar a missao");
        assertEquals("Missao 1 - Fracoes", visiveis.get(0).getTitulo());
    }

    @Test
    @DisplayName("assert(criacaoNegadaParaOutroProfessor)")
    void professorQueNaoEResponsavelNaoPodeCriarMissao() {
        Usuario responsavel = umProfessor("Ana");
        Usuario outroProfessor = umProfessor("Carlos");
        Curso curso = umCurso("Matematica Basica", responsavel);

        assertThrows(AcessoNegadoException.class,
                () -> missaoService.criar(outroProfessor.getId(), curso.getId(), missaoDeExemplo(10)));
        assertTrue(missaoRepository.findByCursoIdOrderByIdAsc(curso.getId()).isEmpty(),
                "nenhuma missao deveria ter sido criada");
    }

    @Test
    @DisplayName("aluno nao pode criar missao")
    void alunoNaoPodeCriarMissao() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);

        assertThrows(AcessoNegadoException.class,
                () -> missaoService.criar(aluno.getId(), curso.getId(), missaoDeExemplo(10)));
    }

    @Test
    @DisplayName("missao sem desafios e rejeitada")
    void missaoPrecisaDeAoMenosUmDesafio() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);

        NovaMissaoRequest semDesafios = new NovaMissaoRequest("Missao vazia", "sem conteudo", List.of());

        assertThrows(RegraDeNegocioException.class,
                () -> missaoService.criar(professor.getId(), curso.getId(), semDesafios));
    }

    @Test
    @DisplayName("desafio com XP zerado e rejeitado")
    void desafioPrecisaDeXpPositivo() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);

        assertThrows(RegraDeNegocioException.class,
                () -> missaoService.criar(professor.getId(), curso.getId(), missaoDeExemplo(0)));
    }

    @Test
    @DisplayName("aluno nao matriculado nao enxerga as missoes do curso")
    void alunoNaoMatriculadoNaoEnxergaMissoes() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario visitante = umAluno("Daniela");
        missaoService.criar(professor.getId(), curso.getId(), missaoDeExemplo(10));

        assertThrows(AcessoNegadoException.class,
                () -> missaoService.listarParaAluno(visitante.getId(), curso.getId()));
    }
}
