package org.facens.grupo_6_gameeducator.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Papel;
import org.facens.grupo_6_gameeducator.domain.Post;
import org.facens.grupo_6_gameeducator.domain.RespostaPost;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.exception.AcessoNegadoException;
import org.facens.grupo_6_gameeducator.service.ForumService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SCENARIO (camada REST): aluno le, publica e responde no forum de um curso.
 * O service e dublado: aqui se testa contrato HTTP, JSON e status.
 */
@WebMvcTest(ForumController.class)
@DisplayName("GET/POST /api/cursos/{cursoId}/forum")
class ForumControllerTest {

    private static final String HEADER = ForumController.HEADER_USUARIO;
    private static final long ALUNO_ID = 2L;
    private static final long CURSO_ID = 10L;
    private static final long POST_ID = 700L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ForumService forumService;

    @Test
    @DisplayName("200 com os posts do forum do curso")
    void deveListarPosts() throws Exception {
        when(forumService.listarPosts(ALUNO_ID, CURSO_ID)).thenReturn(List.of(postSalvo()));

        mockMvc.perform(get("/api/cursos/{cursoId}/forum", CURSO_ID).header(HEADER, ALUNO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Duvida sobre fracoes"))
                .andExpect(jsonPath("$[0].autorNome").value("Bruno"));
    }

    @Test
    @DisplayName("403 quando o usuario nao tem acesso ao curso")
    void deveNegarListagemSemAcesso() throws Exception {
        when(forumService.listarPosts(ALUNO_ID, CURSO_ID))
                .thenThrow(new AcessoNegadoException("Usuario 2 nao tem acesso ao forum do curso 10"));

        mockMvc.perform(get("/api/cursos/{cursoId}/forum", CURSO_ID).header(HEADER, ALUNO_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("200 com o post publicado")
    void devePublicarPost() throws Exception {
        when(forumService.publicar(eq(ALUNO_ID), eq(CURSO_ID), eq("Duvida sobre fracoes"), any()))
                .thenReturn(postSalvo());

        mockMvc.perform(post("/api/cursos/{cursoId}/forum", CURSO_ID)
                        .header(HEADER, ALUNO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "Duvida sobre fracoes", "conteudo": "Como somo 1/2 + 1/4?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Duvida sobre fracoes"));
    }

    @Test
    @DisplayName("200 com a resposta publicada")
    void deveResponderPost() throws Exception {
        when(forumService.responder(eq(ALUNO_ID), eq(POST_ID), any())).thenReturn(respostaSalva());

        mockMvc.perform(post("/api/forum/posts/{postId}/respostas", POST_ID)
                        .header(HEADER, ALUNO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conteudo": "E soma o numerador com o mesmo denominador"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").value("E soma o numerador com o mesmo denominador"))
                .andExpect(jsonPath("$.autorNome").value("Bruno"));
    }

    private static Post postSalvo() {
        Usuario professor = new Usuario("Ana", "ana@facens.br", Papel.PROFESSOR);
        professor.setId(1L);
        Curso curso = new Curso("Matematica Basica", professor);
        curso.setId(CURSO_ID);
        Usuario aluno = new Usuario("Bruno", "bruno@facens.br", Papel.ALUNO);
        aluno.setId(ALUNO_ID);

        Post post = new Post(curso, aluno, "Duvida sobre fracoes", "Como somo 1/2 + 1/4?");
        post.setId(POST_ID);
        return post;
    }

    private static RespostaPost respostaSalva() {
        Usuario aluno = new Usuario("Bruno", "bruno@facens.br", Papel.ALUNO);
        aluno.setId(ALUNO_ID);
        RespostaPost resposta = new RespostaPost(aluno, "E soma o numerador com o mesmo denominador");
        resposta.setId(701L);
        return resposta;
    }
}
