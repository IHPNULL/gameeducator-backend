package org.facens.grupo_6_gameeducator.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Nota;
import org.facens.grupo_6_gameeducator.domain.Papel;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.exception.AcessoNegadoException;
import org.facens.grupo_6_gameeducator.service.NotaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SCENARIO (camada REST): aluno ve as proprias notas do curso; professor responsavel lanca notas.
 * O service e dublado: aqui se testa contrato HTTP, JSON e status.
 */
@WebMvcTest(NotaController.class)
@DisplayName("GET/POST /api/cursos/{cursoId}/notas")
class NotaControllerTest {

    private static final String HEADER = NotaController.HEADER_USUARIO;
    private static final long PROFESSOR_ID = 1L;
    private static final long ALUNO_ID = 2L;
    private static final long CURSO_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotaService notaService;

    @Test
    @DisplayName("200 com as notas do proprio aluno")
    void deveListarNotasDoAluno() throws Exception {
        when(notaService.minhasNotas(ALUNO_ID, CURSO_ID)).thenReturn(List.of(notaSalva()));

        mockMvc.perform(get("/api/cursos/{cursoId}/notas", CURSO_ID).header(HEADER, ALUNO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].avaliacao").value("Prova 1"))
                .andExpect(jsonPath("$[0].valor").value(8.5));
    }

    @Test
    @DisplayName("403 quando o aluno nao esta matriculado")
    void deveNegarListagemParaAlunoNaoMatriculado() throws Exception {
        when(notaService.minhasNotas(ALUNO_ID, CURSO_ID))
                .thenThrow(new AcessoNegadoException("Aluno 2 nao esta matriculado no curso 10"));

        mockMvc.perform(get("/api/cursos/{cursoId}/notas", CURSO_ID).header(HEADER, ALUNO_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("200 com a nota lancada pelo professor")
    void deveLancarNotaDoAluno() throws Exception {
        when(notaService.lancar(eq(PROFESSOR_ID), eq(CURSO_ID), eq(ALUNO_ID), eq("Prova 1"), any()))
                .thenReturn(notaSalva());

        mockMvc.perform(post("/api/cursos/{cursoId}/notas/{alunoId}", CURSO_ID, ALUNO_ID)
                        .header(HEADER, PROFESSOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"avaliacao": "Prova 1", "valor": 8.5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avaliacao").value("Prova 1"))
                .andExpect(jsonPath("$.valor").value(8.5));
    }

    @Test
    @DisplayName("403 quando quem lanca nao e o professor responsavel")
    void deveNegarLancamentoParaProfessorQueNaoEResponsavel() throws Exception {
        when(notaService.lancar(eq(PROFESSOR_ID), eq(CURSO_ID), eq(ALUNO_ID), any(), any()))
                .thenThrow(new AcessoNegadoException("Somente o professor responsavel lanca notas no curso 10"));

        mockMvc.perform(post("/api/cursos/{cursoId}/notas/{alunoId}", CURSO_ID, ALUNO_ID)
                        .header(HEADER, PROFESSOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"avaliacao": "Prova 1", "valor": 8.5}
                                """))
                .andExpect(status().isForbidden());
    }

    private static Nota notaSalva() {
        Usuario professor = new Usuario("Ana", "ana@facens.br", Papel.PROFESSOR);
        professor.setId(PROFESSOR_ID);
        Curso curso = new Curso("Matematica Basica", professor);
        curso.setId(CURSO_ID);
        Usuario aluno = new Usuario("Bruno", "bruno@facens.br", Papel.ALUNO);
        aluno.setId(ALUNO_ID);

        Nota nota = new Nota(aluno, curso, "Prova 1", new BigDecimal("8.5"));
        nota.setId(500L);
        return nota;
    }
}
