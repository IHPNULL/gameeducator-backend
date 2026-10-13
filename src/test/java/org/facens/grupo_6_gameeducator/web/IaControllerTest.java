package org.facens.grupo_6_gameeducator.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.facens.grupo_6_gameeducator.exception.ServicoIndisponivelException;
import org.facens.grupo_6_gameeducator.ia.RespostaDoAssistente;
import org.facens.grupo_6_gameeducator.service.AssistenteService;
import org.facens.grupo_6_gameeducator.service.RecomendacaoService;
import org.facens.grupo_6_gameeducator.service.dto.Recomendacoes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IaController.class)
@DisplayName("Endpoints de IA do Core")
class IaControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private RecomendacaoService recomendacaoService;
    @MockitoBean
    private AssistenteService assistenteService;

    @Test
    @DisplayName("GET recomendacoes -> 200 com a origem (ia | fallback)")
    void recomendacoes() throws Exception {
        when(recomendacaoService.recomendar(2L, 10L)).thenReturn(new Recomendacoes(Recomendacoes.ORIGEM_FALLBACK, null,
                List.of(new Recomendacoes.Item(5L, "Decimais", 0.0, "local"))));

        mockMvc.perform(get("/api/cursos/{cursoId}/recomendacoes", 10L).header(IaController.HEADER_USUARIO, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origem").value("fallback"))
                .andExpect(jsonPath("$.itens[0].missaoId").value(5));
    }

    @Test
    @DisplayName("POST assistente -> 200 com resposta e fontes")
    void assistente() throws Exception {
        when(assistenteService.perguntar(2L, 10L, "o que e fracao?")).thenReturn(new RespostaDoAssistente("r",
                List.of(new RespostaDoAssistente.Fonte("missao-1", "Fracoes", 0.4)), "extrativo"));

        mockMvc.perform(post("/api/cursos/{cursoId}/assistente", 10L).header(IaController.HEADER_USUARIO, 2L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pergunta\":\"o que e fracao?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fontes[0].id").value("missao-1"));
    }

    @Test
    @DisplayName("POST assistente com a IA fora -> 503")
    void assistenteIndisponivel() throws Exception {
        when(assistenteService.perguntar(2L, 10L, "?")).thenThrow(new ServicoIndisponivelException("indisponivel"));

        mockMvc.perform(post("/api/cursos/{cursoId}/assistente", 10L).header(IaController.HEADER_USUARIO, 2L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pergunta\":\"?\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    @DisplayName("POST assistente com pergunta vazia -> 400")
    void perguntaVazia() throws Exception {
        mockMvc.perform(post("/api/cursos/{cursoId}/assistente", 10L).header(IaController.HEADER_USUARIO, 2L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pergunta\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
