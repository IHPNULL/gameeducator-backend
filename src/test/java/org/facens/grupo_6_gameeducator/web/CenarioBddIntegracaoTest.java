package org.facens.grupo_6_gameeducator.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Matricula;
import org.facens.grupo_6_gameeducator.domain.Papel;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.repository.CursoRepository;
import org.facens.grupo_6_gameeducator.repository.MatriculaRepository;
import org.facens.grupo_6_gameeducator.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cenario BDD completo passando pela API real: HTTP -> controller -> service -> banco.
 *
 * <p>Dado que estou autenticado como professor responsavel por um curso, quando crio uma missao,
 * entao o aluno matriculado a enxerga, responde e recebe o XP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Cenario BDD ponta a ponta via API")
class CenarioBddIntegracaoTest {

    private static final String HEADER = MissaoController.HEADER_USUARIO;

    private static final String NOVA_MISSAO = """
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
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CursoRepository cursoRepository;
    @Autowired
    private MatriculaRepository matriculaRepository;

    @Test
    @DisplayName("professor publica a missao, aluno matriculado joga e ganha XP")
    void cicloCompletoDaSprint() throws Exception {
        // Dado que o professor e responsavel por um curso e o aluno esta matriculado nele
        Usuario professor = usuarioRepository.save(new Usuario("Ana", "ana.e2e@facens.br", Papel.PROFESSOR));
        Usuario aluno = usuarioRepository.save(new Usuario("Bruno", "bruno.e2e@facens.br", Papel.ALUNO));
        Curso curso = cursoRepository.save(new Curso("Matematica Basica", professor));
        matriculaRepository.save(new Matricula(curso, aluno));

        // Quando o professor cria a missao com seus desafios
        String criada = mockMvc.perform(post("/api/cursos/{cursoId}/missoes", curso.getId())
                        .header(HEADER, professor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NOVA_MISSAO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.xpTotal").value(10))
                .andReturn().getResponse().getContentAsString();
        Number desafioId = JsonPath.read(criada, "$.desafios[0].id");

        // Entao a missao fica visivel para o aluno matriculado, sem revelar a alternativa correta
        mockMvc.perform(get("/api/cursos/{cursoId}/missoes", curso.getId()).header(HEADER, aluno.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Missao 1 - Fracoes"))
                .andExpect(jsonPath("$[0].desafios[0].indiceRespostaCorreta").doesNotExist());

        // E o aluno que erra nao recebe XP
        mockMvc.perform(post("/api/desafios/{desafioId}/respostas", desafioId.longValue())
                        .header(HEADER, aluno.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"indiceResposta": 0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correta").value(false))
                .andExpect(jsonPath("$.xpGanho").value(0))
                .andExpect(jsonPath("$.xpTotalNoCurso").value(0));

        // E ao acertar recebe o XP do desafio, refletido no progresso e no ranking
        mockMvc.perform(post("/api/desafios/{desafioId}/respostas", desafioId.longValue())
                        .header(HEADER, aluno.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"indiceResposta": 1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correta").value(true))
                .andExpect(jsonPath("$.xpGanho").value(10))
                .andExpect(jsonPath("$.xpTotalNoCurso").value(10));

        mockMvc.perform(get("/api/cursos/{cursoId}/progresso", curso.getId()).header(HEADER, aluno.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.xpTotal").value(10));

        mockMvc.perform(get("/api/desafios/{desafioId}/tentativas", desafioId.longValue())
                        .header(HEADER, aluno.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/cursos/{cursoId}/ranking", curso.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nomeAluno").value("Bruno"))
                .andExpect(jsonPath("$[0].xpTotal").value(10))
                .andExpect(jsonPath("$[0].posicao").value(1));
    }

    @Test
    @DisplayName("professor que nao e responsavel pelo curso recebe 403 e nada e publicado")
    void professorSemVinculoNaoPublica() throws Exception {
        Usuario responsavel = usuarioRepository.save(new Usuario("Ana", "ana.403@facens.br", Papel.PROFESSOR));
        Usuario intruso = usuarioRepository.save(new Usuario("Carlos", "carlos.403@facens.br", Papel.PROFESSOR));
        Curso curso = cursoRepository.save(new Curso("Matematica Basica", responsavel));

        mockMvc.perform(post("/api/cursos/{cursoId}/missoes", curso.getId())
                        .header(HEADER, intruso.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NOVA_MISSAO))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/cursos/{cursoId}/missoes", curso.getId()).header(HEADER, responsavel.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
