package org.facens.grupo_6_gameeducator.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Desafio;
import org.facens.grupo_6_gameeducator.domain.Medalha;
import org.facens.grupo_6_gameeducator.domain.Missao;
import org.facens.grupo_6_gameeducator.domain.Papel;
import org.facens.grupo_6_gameeducator.domain.ProgressoAluno;
import org.facens.grupo_6_gameeducator.domain.Tentativa;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.exception.AcessoNegadoException;
import org.facens.grupo_6_gameeducator.exception.RecursoNaoEncontradoException;
import org.facens.grupo_6_gameeducator.service.JogoService;
import org.facens.grupo_6_gameeducator.service.dto.ResultadoResposta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SCENARIO (camada REST): aluno responde um desafio e recebe o XP.
 * O service e dublado: aqui se testa contrato HTTP, JSON e status.
 */
@WebMvcTest(JogoController.class)
@DisplayName("Endpoints do aluno jogando")
class JogoControllerTest {

    private static final String HEADER = JogoController.HEADER_USUARIO;
    private static final long ALUNO_ID = 2L;
    private static final long CURSO_ID = 10L;
    private static final long DESAFIO_ID = 1000L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JogoService jogoService;

    @Test
    @DisplayName("POST resposta correta -> 200 com correta=true e o XP do desafio")
    void acertoRetornaXpDoDesafio() throws Exception {
        when(jogoService.responder(ALUNO_ID, DESAFIO_ID, 1)).thenReturn(new ResultadoResposta(true, 10, 10));

        mockMvc.perform(post("/api/desafios/{desafioId}/respostas", DESAFIO_ID)
                        .header(HEADER, ALUNO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"indiceResposta": 1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correta").value(true))
                .andExpect(jsonPath("$.xpGanho").value(10))
                .andExpect(jsonPath("$.xpTotalNoCurso").value(10));

        verify(jogoService).responder(ALUNO_ID, DESAFIO_ID, 1);
    }

    @Test
    @DisplayName("POST resposta errada -> 200 com correta=false e xpGanho=0")
    void erroNaoCreditaXp() throws Exception {
        when(jogoService.responder(ALUNO_ID, DESAFIO_ID, 0)).thenReturn(new ResultadoResposta(false, 0, 0));

        mockMvc.perform(post("/api/desafios/{desafioId}/respostas", DESAFIO_ID)
                        .header(HEADER, ALUNO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"indiceResposta": 0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correta").value(false))
                .andExpect(jsonPath("$.xpGanho").value(0));
    }

    @Test
    @DisplayName("403 quando o aluno nao esta matriculado no curso")
    void alunoNaoMatriculadoRecebe403() throws Exception {
        when(jogoService.responder(ALUNO_ID, DESAFIO_ID, 1))
                .thenThrow(new AcessoNegadoException("Aluno 2 nao esta matriculado no curso 10"));

        mockMvc.perform(post("/api/desafios/{desafioId}/respostas", DESAFIO_ID)
                        .header(HEADER, ALUNO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"indiceResposta": 1}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("404 quando o desafio nao existe")
    void desafioInexistenteRetorna404() throws Exception {
        when(jogoService.responder(ALUNO_ID, 999L, 1))
                .thenThrow(RecursoNaoEncontradoException.de("Desafio", 999L));

        mockMvc.perform(post("/api/desafios/{desafioId}/respostas", 999L)
                        .header(HEADER, ALUNO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"indiceResposta": 1}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("400 quando o corpo nao traz a alternativa escolhida")
    void corpoSemIndiceRetorna400() throws Exception {
        mockMvc.perform(post("/api/desafios/{desafioId}/respostas", DESAFIO_ID)
                        .header(HEADER, ALUNO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET progresso -> XP acumulado do aluno no curso")
    void deveRetornarProgressoDoAluno() throws Exception {
        when(jogoService.xpNoCurso(ALUNO_ID, CURSO_ID)).thenReturn(25);

        mockMvc.perform(get("/api/cursos/{cursoId}/progresso", CURSO_ID).header(HEADER, ALUNO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cursoId").value(CURSO_ID))
                .andExpect(jsonPath("$.alunoId").value(ALUNO_ID))
                .andExpect(jsonPath("$.xpTotal").value(25));
    }

    @Test
    @DisplayName("GET tentativas -> historico do aluno no desafio")
    void deveRetornarHistoricoDeTentativas() throws Exception {
        Usuario aluno = aluno();
        Desafio desafio = desafio();
        Tentativa erro = new Tentativa(aluno, desafio, 0, false, 0);
        erro.setId(1L);
        Tentativa acerto = new Tentativa(aluno, desafio, 1, true, 10);
        acerto.setId(2L);
        when(jogoService.historico(ALUNO_ID, DESAFIO_ID)).thenReturn(List.of(acerto, erro));

        mockMvc.perform(get("/api/desafios/{desafioId}/tentativas", DESAFIO_ID).header(HEADER, ALUNO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].correta").value(true))
                .andExpect(jsonPath("$[0].xpGanho").value(10))
                .andExpect(jsonPath("$[1].correta").value(false));
    }

    @Test
    @DisplayName("GET ranking -> turma ordenada por XP, com posicao")
    void deveRetornarRankingDaTurma() throws Exception {
        Curso curso = curso();
        ProgressoAluno primeiro = progresso(usuario(2L, "Bruno"), curso, 30);
        ProgressoAluno segundo = progresso(usuario(3L, "Daniela"), curso, 10);
        when(jogoService.ranking(CURSO_ID)).thenReturn(List.of(primeiro, segundo));

        mockMvc.perform(get("/api/cursos/{cursoId}/ranking", CURSO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].posicao").value(1))
                .andExpect(jsonPath("$[0].nomeAluno").value("Bruno"))
                .andExpect(jsonPath("$[0].xpTotal").value(30))
                .andExpect(jsonPath("$[1].posicao").value(2))
                .andExpect(jsonPath("$[1].nomeAluno").value("Daniela"));
    }

    @Test
    @DisplayName("GET medalhas -> marcos de XP conquistados pelo aluno no curso")
    void deveRetornarMedalhasDoAluno() throws Exception {
        Usuario aluno = aluno();
        Curso curso = curso();
        Medalha medalha = new Medalha(aluno, curso, 50);
        when(jogoService.medalhasDoAluno(ALUNO_ID, CURSO_ID)).thenReturn(List.of(medalha));

        mockMvc.perform(get("/api/cursos/{cursoId}/medalhas", CURSO_ID).header(HEADER, ALUNO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].marcoXp").value(50));
    }

    private static Usuario aluno() {
        return usuario(ALUNO_ID, "Bruno");
    }

    private static Usuario usuario(long id, String nome) {
        Usuario usuario = new Usuario(nome, nome.toLowerCase() + "@facens.br", Papel.ALUNO);
        usuario.setId(id);
        return usuario;
    }

    private static Curso curso() {
        Usuario professor = new Usuario("Ana", "ana@facens.br", Papel.PROFESSOR);
        professor.setId(1L);
        Curso curso = new Curso("Matematica Basica", professor);
        curso.setId(CURSO_ID);
        return curso;
    }

    private static Desafio desafio() {
        Missao missao = new Missao("Missao 1 - Fracoes", "desafios", curso());
        missao.setId(100L);
        Desafio desafio = new Desafio("Quanto e 1/2 + 1/4 ?", List.of("1/6", "3/4"), 1, 10);
        desafio.setId(DESAFIO_ID);
        missao.adicionarDesafio(desafio);
        return desafio;
    }

    private static ProgressoAluno progresso(Usuario aluno, Curso curso, int xp) {
        ProgressoAluno progresso = new ProgressoAluno(aluno, curso);
        progresso.creditarXp(xp);
        return progresso;
    }
}
