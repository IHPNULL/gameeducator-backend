package org.facens.grupo_6_gameeducator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Nota;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.exception.AcessoNegadoException;
import org.facens.grupo_6_gameeducator.exception.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * BDD: Dado que estou autenticado como aluno E estou matriculado em um curso,
 * quando clico no curso E clico na aba de notas, entao o sistema exibe as minhas
 * notas daquele curso E as notas devem corresponder aos meus resultados.
 */
@DisplayName("Aluno visualiza suas notas de um curso")
class AlunoVeNotasTest extends CenarioBase {

    @Test
    @DisplayName("assert(notasExibidas === notasEsperadas)")
    void deveExibirAsNotasDoAlunoNaqueleCurso() {
        // Dado que estou autenticado como aluno E estou matriculado em um curso
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);

        // E o professor lancou os meus resultados
        notaService.lancar(professor.getId(), curso.getId(), aluno.getId(), "Prova 1", new BigDecimal("8.50"));
        notaService.lancar(professor.getId(), curso.getId(), aluno.getId(), "Prova 2", new BigDecimal("7.00"));
        notaService.lancar(professor.getId(), curso.getId(), aluno.getId(), "Trabalho Final", new BigDecimal("9.25"));

        // Quando clico no curso E clico na aba de notas
        List<Nota> notasExibidas = notaService.minhasNotas(aluno.getId(), curso.getId());

        // Entao o sistema exibe as minhas notas daquele curso
        assertEquals(3, notasExibidas.size());
        assertEquals(List.of("Prova 1", "Prova 2", "Trabalho Final"),
                notasExibidas.stream().map(Nota::getAvaliacao).toList());

        // E as notas correspondem aos meus resultados
        assertEquals(List.of(new BigDecimal("8.50"), new BigDecimal("7.00"), new BigDecimal("9.25")),
                notasExibidas.stream().map(Nota::getValor).toList());
    }

    @Test
    @DisplayName("as notas exibidas sao apenas daquele curso")
    void naoDeveMisturarNotasDeOutroCurso() {
        Usuario professor = umProfessor("Ana");
        Curso matematica = umCurso("Matematica Basica", professor);
        Curso portugues = umCurso("Portugues", professor);
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, matematica);
        matricular(aluno, portugues);

        notaService.lancar(professor.getId(), matematica.getId(), aluno.getId(), "Prova 1", new BigDecimal("8.50"));
        notaService.lancar(professor.getId(), portugues.getId(), aluno.getId(), "Prova 1", new BigDecimal("6.00"));

        List<Nota> notasDeMatematica = notaService.minhasNotas(aluno.getId(), matematica.getId());

        assertEquals(1, notasDeMatematica.size());
        assertEquals(0, new BigDecimal("8.50").compareTo(notasDeMatematica.get(0).getValor()));
    }

    @Test
    @DisplayName("as notas exibidas sao apenas as minhas")
    void naoDeveExibirNotasDeOutroAluno() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario bruno = umAluno("Bruno");
        Usuario daniela = umAluno("Daniela");
        matricular(bruno, curso);
        matricular(daniela, curso);

        notaService.lancar(professor.getId(), curso.getId(), bruno.getId(), "Prova 1", new BigDecimal("8.50"));
        notaService.lancar(professor.getId(), curso.getId(), daniela.getId(), "Prova 1", new BigDecimal("10.00"));

        List<Nota> notasDeBruno = notaService.minhasNotas(bruno.getId(), curso.getId());

        assertEquals(1, notasDeBruno.size());
        assertEquals(0, new BigDecimal("8.50").compareTo(notasDeBruno.get(0).getValor()));
        assertEquals(bruno.getId(), notasDeBruno.get(0).getAluno().getId());
    }

    @Test
    @DisplayName("aluno sem matricula no curso nao acessa a aba de notas")
    void alunoNaoMatriculadoNaoVeNotas() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario visitante = umAluno("Daniela");

        assertThrows(AcessoNegadoException.class,
                () -> notaService.minhasNotas(visitante.getId(), curso.getId()));
    }

    @Test
    @DisplayName("aluno matriculado sem notas lancadas ve a aba vazia")
    void alunoSemNotasVeListaVazia() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);

        assertTrue(notaService.minhasNotas(aluno.getId(), curso.getId()).isEmpty());
    }

    @Test
    @DisplayName("relancar a mesma avaliacao corrige a nota, nao duplica")
    void relancarAvaliacaoAtualizaANota() {
        Usuario professor = umProfessor("Ana");
        Curso curso = umCurso("Matematica Basica", professor);
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);

        notaService.lancar(professor.getId(), curso.getId(), aluno.getId(), "Prova 1", new BigDecimal("5.00"));
        notaService.lancar(professor.getId(), curso.getId(), aluno.getId(), "Prova 1", new BigDecimal("8.50"));

        List<Nota> notas = notaService.minhasNotas(aluno.getId(), curso.getId());
        assertEquals(1, notas.size(), "corrigir a nota nao pode criar uma segunda linha");
        assertEquals(0, new BigDecimal("8.50").compareTo(notas.get(0).getValor()));
    }

    @Test
    @DisplayName("so o professor responsavel lanca nota, e apenas entre 0 e 10")
    void lancamentoDeNotaEProtegido() {
        Usuario responsavel = umProfessor("Ana");
        Usuario outroProfessor = umProfessor("Carlos");
        Curso curso = umCurso("Matematica Basica", responsavel);
        Usuario aluno = umAluno("Bruno");
        matricular(aluno, curso);

        assertThrows(AcessoNegadoException.class, () -> notaService.lancar(
                outroProfessor.getId(), curso.getId(), aluno.getId(), "Prova 1", new BigDecimal("8.50")));
        assertThrows(RegraDeNegocioException.class, () -> notaService.lancar(
                responsavel.getId(), curso.getId(), aluno.getId(), "Prova 1", new BigDecimal("11.00")));
    }
}
