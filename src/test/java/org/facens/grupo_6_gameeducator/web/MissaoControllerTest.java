package org.facens.grupo_6_gameeducator.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Desafio;
import org.facens.grupo_6_gameeducator.domain.Missao;
import org.facens.grupo_6_gameeducator.domain.Papel;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.exception.AcessoNegadoException;
import org.facens.grupo_6_gameeducator.exception.RegraDeNegocioException;
import org.facens.grupo_6_gameeducator.service.MissaoService;
import org.facens.grupo_6_gameeducator.service.dto.NovaMissaoRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SCENARIO (camada REST): professor cria uma missao com desafios no seu curso.
 * O service e dublado: aqui se testa contrato HTTP, JSON e status.
 */
@WebMvcTest(MissaoController.class)
@DisplayName("POST/GET /api/cursos/{cursoId}/missoes")
class MissaoControllerTest {

    private static final String HEADER = MissaoController.HEADER_USUARIO;
    private static final long PROFESSOR_ID = 1L;
    private static final long ALUNO_ID = 2L;
    private static final long CURSO_ID = 10L;

    private static final String JSON_NOVA_MISSAO = """
            {
              "titulo": "Missao 1 - Fracoes",
              "descricao": "Resolva os desafios sobre fracoes",
              "desafios": [
                {
                  "enunciado": "Quanto e 1/2 + 1/4 ?",
                  "alternativas": ["1/6", "3/4", "2/6", "1/8"],
                  "indiceRespostaCorreta": 1,
                  "xp": 10
                }
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MissaoService missaoService;

    @Test
    @DisplayName("201 Created com a missao e seus desafios")
    void deveCriarMissao() throws Exception {
        when(missaoService.criar(eq(PROFESSOR_ID), eq(CURSO_ID), any())).thenReturn(missaoSalva());

        mockMvc.perform(post("/api/cursos/{cursoId}/missoes", CURSO_ID)
                        .header(HEADER, PROFESSOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_NOVA_MISSAO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/cursos/10/missoes/100"))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.titulo").value("Missao 1 - Fracoes"))
                .andExpect(jsonPath("$.xpTotal").value(10))
                .andExpect(jsonPath("$.desafios.length()").value(1))
                .andExpect(jsonPath("$.desafios[0].enunciado").value("Quanto e 1/2 + 1/4 ?"))
                .andExpect(jsonPath("$.desafios[0].alternativas.length()").value(4))
                .andExpect(jsonPath("$.desafios[0].xp").value(10));
    }

    @Test
    @DisplayName("o corpo enviado chega integro ao service")
    void deveRepassarOsDadosDoFormularioParaOService() throws Exception {
        when(missaoService.criar(eq(PROFESSOR_ID), eq(CURSO_ID), any())).thenReturn(missaoSalva());

        mockMvc.perform(post("/api/cursos/{cursoId}/missoes", CURSO_ID)
                        .header(HEADER, PROFESSOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_NOVA_MISSAO))
                .andExpect(status().isCreated());

        ArgumentCaptor<NovaMissaoRequest> captor = ArgumentCaptor.forClass(NovaMissaoRequest.class);
        verify(missaoService).criar(eq(PROFESSOR_ID), eq(CURSO_ID), captor.capture());
        NovaMissaoRequest enviado = captor.getValue();
        assertEquals("Missao 1 - Fracoes", enviado.titulo());
        assertEquals(1, enviado.desafios().size());
        assertEquals(1, enviado.desafios().get(0).indiceRespostaCorreta());
        assertEquals(10, enviado.desafios().get(0).xp());
    }

    @Test
    @DisplayName("403 quando o professor nao e responsavel pelo curso")
    void deveNegarCriacaoParaProfessorQueNaoEResponsavel() throws Exception {
        when(missaoService.criar(eq(PROFESSOR_ID), eq(CURSO_ID), any()))
                .thenThrow(new AcessoNegadoException("Professor nao e responsavel pelo curso 10"));

        mockMvc.perform(post("/api/cursos/{cursoId}/missoes", CURSO_ID)
                        .header(HEADER, PROFESSOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_NOVA_MISSAO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensagem").value("Professor nao e responsavel pelo curso 10"));
    }

    @Test
    @DisplayName("400 quando a missao nao tem desafios")
    void deveRetornar400ParaMissaoInvalida() throws Exception {
        when(missaoService.criar(eq(PROFESSOR_ID), eq(CURSO_ID), any()))
                .thenThrow(new RegraDeNegocioException("A missao precisa de ao menos um desafio"));

        mockMvc.perform(post("/api/cursos/{cursoId}/missoes", CURSO_ID)
                        .header(HEADER, PROFESSOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "Missao vazia", "descricao": "sem conteudo", "desafios": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("400 lista todos os campos invalidos de uma vez (Bean Validation)")
    void deveListarTodosOsCamposInvalidosDeUmaVez() throws Exception {
        mockMvc.perform(post("/api/cursos/{cursoId}/missoes", CURSO_ID)
                        .header(HEADER, PROFESSOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "", "descricao": "sem conteudo", "desafios": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(result -> {
                    String mensagem = result.getResponse().getContentAsString();
                    org.junit.jupiter.api.Assertions.assertTrue(mensagem.contains("titulo"),
                            "deveria citar o campo titulo: " + mensagem);
                    org.junit.jupiter.api.Assertions.assertTrue(mensagem.contains("desafios"),
                            "deveria citar o campo desafios: " + mensagem);
                });
    }

    @Test
    @DisplayName("400 quando falta o header de usuario autenticado")
    void deveExigirUsuarioAutenticado() throws Exception {
        mockMvc.perform(post("/api/cursos/{cursoId}/missoes", CURSO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_NOVA_MISSAO))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("200 com as missoes visiveis para o aluno matriculado")
    void deveListarMissoesParaOAluno() throws Exception {
        when(missaoService.listarParaUsuario(ALUNO_ID, CURSO_ID)).thenReturn(List.of(missaoSalva()));

        mockMvc.perform(get("/api/cursos/{cursoId}/missoes", CURSO_ID).header(HEADER, ALUNO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Missao 1 - Fracoes"));
    }

    @Test
    @DisplayName("a resposta correta do desafio nunca vai no JSON")
    void naoDeveExporAlternativaCorreta() throws Exception {
        when(missaoService.listarParaUsuario(ALUNO_ID, CURSO_ID)).thenReturn(List.of(missaoSalva()));

        mockMvc.perform(get("/api/cursos/{cursoId}/missoes", CURSO_ID).header(HEADER, ALUNO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].desafios[0].indiceRespostaCorreta").doesNotExist());
    }

    @Test
    @DisplayName("403 quando o aluno nao esta matriculado")
    void deveNegarListagemParaAlunoNaoMatriculado() throws Exception {
        when(missaoService.listarParaUsuario(ALUNO_ID, CURSO_ID))
                .thenThrow(new AcessoNegadoException("Aluno 2 nao esta matriculado no curso 10"));

        mockMvc.perform(get("/api/cursos/{cursoId}/missoes", CURSO_ID).header(HEADER, ALUNO_ID))
                .andExpect(status().isForbidden());
    }

    private static Missao missaoSalva() {
        Usuario professor = new Usuario("Ana", "ana@facens.br", Papel.PROFESSOR);
        professor.setId(PROFESSOR_ID);
        Curso curso = new Curso("Matematica Basica", professor);
        curso.setId(CURSO_ID);

        Missao missao = new Missao("Missao 1 - Fracoes", "Resolva os desafios sobre fracoes", curso);
        missao.setId(100L);
        Desafio desafio = new Desafio("Quanto e 1/2 + 1/4 ?", List.of("1/6", "3/4", "2/6", "1/8"), 1, 10);
        desafio.setId(1000L);
        missao.adicionarDesafio(desafio);
        return missao;
    }
}
