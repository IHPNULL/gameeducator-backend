package org.facens.grupo_6_gameeducator.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Papel;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.exception.AcessoNegadoException;
import org.facens.grupo_6_gameeducator.service.JogoService;
import org.facens.grupo_6_gameeducator.service.dto.DesempenhoAluno;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SCENARIO (camada REST): professor acompanha o desempenho da turma no curso.
 * O service e dublado: aqui se testa contrato HTTP, JSON e status.
 */
@WebMvcTest(TurmaController.class)
@DisplayName("GET /api/cursos/{cursoId}/turma/desempenho")
class TurmaControllerTest {

    private static final String HEADER = TurmaController.HEADER_USUARIO;
    private static final long PROFESSOR_ID = 1L;
    private static final long CURSO_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JogoService jogoService;

    @Test
    @DisplayName("200 com o desempenho de cada aluno matriculado")
    void deveRetornarDesempenhoDaTurma() throws Exception {
        Usuario aluno = new Usuario("Bruno", "bruno@facens.br", Papel.ALUNO);
        aluno.setId(2L);
        when(jogoService.desempenhoDaTurma(eq(PROFESSOR_ID), eq(CURSO_ID)))
                .thenReturn(List.of(new DesempenhoAluno(aluno, 10, 2, 1)));

        mockMvc.perform(get("/api/cursos/{cursoId}/turma/desempenho", CURSO_ID).header(HEADER, PROFESSOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Bruno"))
                .andExpect(jsonPath("$[0].xpTotal").value(10))
                .andExpect(jsonPath("$[0].tentativas").value(2))
                .andExpect(jsonPath("$[0].acertos").value(1))
                .andExpect(jsonPath("$[0].taxaAcerto").value(0.5));
    }

    @Test
    @DisplayName("403 quando quem consulta nao e o professor responsavel")
    void deveNegarParaProfessorQueNaoEResponsavel() throws Exception {
        when(jogoService.desempenhoDaTurma(eq(PROFESSOR_ID), eq(CURSO_ID)))
                .thenThrow(new AcessoNegadoException("Somente o professor responsavel acompanha a turma do curso 10"));

        mockMvc.perform(get("/api/cursos/{cursoId}/turma/desempenho", CURSO_ID).header(HEADER, PROFESSOR_ID))
                .andExpect(status().isForbidden());
    }
}
