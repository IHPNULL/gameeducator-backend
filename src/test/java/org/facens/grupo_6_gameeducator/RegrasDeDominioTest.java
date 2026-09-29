package org.facens.grupo_6_gameeducator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Desafio;
import org.facens.grupo_6_gameeducator.domain.Nota;
import org.facens.grupo_6_gameeducator.domain.Papel;
import org.facens.grupo_6_gameeducator.domain.ProgressoAluno;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guardas das proprias entidades, sem Spring nem banco.
 * Sao os caminhos que os services nao alcancam porque validam antes.
 */
@DisplayName("Regras das entidades de dominio")
class RegrasDeDominioTest {

    private static Usuario professor(long id) {
        Usuario usuario = new Usuario("Ana", "ana@facens.br", Papel.PROFESSOR);
        usuario.setId(id);
        return usuario;
    }

    private static Usuario aluno(long id) {
        Usuario usuario = new Usuario("Bruno", "bruno@facens.br", Papel.ALUNO);
        usuario.setId(id);
        return usuario;
    }

    @Test
    @DisplayName("papel do usuario distingue aluno de professor")
    void papelDoUsuario() {
        assertTrue(professor(1L).isProfessor());
        assertFalse(professor(1L).isAluno());
        assertTrue(aluno(2L).isAluno());
        assertFalse(aluno(2L).isProfessor());
    }

    @Test
    @DisplayName("curso sem usuario informado nao tem responsavel")
    void cursoNaoEhResponsavelDeUsuarioNulo() {
        Curso curso = new Curso("Matematica Basica", professor(1L));

        assertFalse(curso.ehResponsavel(null), "usuario nulo nunca pode ser responsavel");
        assertTrue(curso.ehResponsavel(professor(1L)));
        assertFalse(curso.ehResponsavel(professor(9L)));
    }

    @Test
    @DisplayName("nota nula e aceita pela entidade e normalizada em 2 casas")
    void notaNulaEArredondamento() {
        Curso curso = new Curso("Matematica Basica", professor(1L));

        assertNull(new Nota(aluno(2L), curso, "Prova 1", null).getValor());
        assertEquals(new BigDecimal("8.50"), new Nota(aluno(2L), curso, "Prova 1", new BigDecimal("8.5")).getValor());
        assertEquals(new BigDecimal("8.46"), new Nota(aluno(2L), curso, "Prova 1", new BigDecimal("8.455")).getValor());
    }

    @Test
    @DisplayName("progresso recusa credito de XP negativo")
    void progressoRecusaXpNegativo() {
        ProgressoAluno progresso = new ProgressoAluno(aluno(2L), new Curso("Matematica Basica", professor(1L)));
        progresso.creditarXp(10);

        assertThrows(IllegalArgumentException.class, () -> progresso.creditarXp(-1));
        assertEquals(10, progresso.getXpTotal(), "o XP nao pode ter sido alterado pela tentativa invalida");
    }

    @Test
    @DisplayName("desafio compara a alternativa escolhida com o gabarito")
    void desafioValidaAlternativa() {
        Desafio desafio = new Desafio("Quanto e 1/2 + 1/4 ?", List.of("1/6", "3/4"), 1, 10);

        assertTrue(desafio.estaCorreta(1));
        assertFalse(desafio.estaCorreta(0));
    }
}
