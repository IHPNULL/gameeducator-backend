package org.facens.grupo_6_gameeducator.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.facens.grupo_6_gameeducator.domain.AcaoDoAgente;
import org.facens.grupo_6_gameeducator.domain.MetaDeEstudo;
import org.facens.grupo_6_gameeducator.exception.AcessoNegadoException;
import org.facens.grupo_6_gameeducator.exception.ServicoIndisponivelException;
import org.facens.grupo_6_gameeducator.service.agente.AcoesDoAgenteService;
import org.facens.grupo_6_gameeducator.service.agente.AgenteService;
import org.facens.grupo_6_gameeducator.service.agente.AgenteService.RespostaDoAgente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AgenteController.class)
@DisplayName("Endpoints dos agentes de IA (RF04)")
class AgenteControllerTest {

    private static final String BASE = "/api/cursos/{cursoId}/agente";
    private static final Instant AGORA = Instant.parse("2026-11-24T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AgenteService agenteService;
    @MockitoBean
    private AcoesDoAgenteService acoesService;

    private static AcaoDoAgente meta() {
        return AcaoDoAgente.metaDeEstudo(2L, 10L, 5L, "Fracoes", LocalDate.of(2026, 12, 1), "terminar", AGORA);
    }

    @Test
    @DisplayName("POST mensagens -> 200 com o agente, a resposta e as acoes pendentes")
    void conversar() throws Exception {
        when(agenteService.conversar(2L, 10L, "o que estudo?"))
                .thenReturn(new RespostaDoAgente(AgenteService.ORIENTACAO, "Estude fracoes", List.of(meta())));

        mockMvc.perform(post(BASE + "/mensagens", 10L).header(AgenteController.HEADER_USUARIO, 2L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mensagem\":\"o que estudo?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agente").value("orientacao"))
                .andExpect(jsonPath("$.resposta").value("Estude fracoes"))
                .andExpect(jsonPath("$.acoesPendentes[0].tipo").value("META_DE_ESTUDO"))
                .andExpect(jsonPath("$.acoesPendentes[0].status").value("PENDENTE"))
                .andExpect(jsonPath("$.acoesPendentes[0].prazo").value("2026-12-01"));
    }

    @Test
    @DisplayName("POST mensagens: vazia -> 400; LLM fora -> 503; fora do curso -> 403")
    void conversarErros() throws Exception {
        mockMvc.perform(post(BASE + "/mensagens", 10L).header(AgenteController.HEADER_USUARIO, 2L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mensagem\":\" \"}"))
                .andExpect(status().isBadRequest());

        when(agenteService.conversar(2L, 10L, "oi")).thenThrow(new ServicoIndisponivelException("indisponivel"));
        mockMvc.perform(post(BASE + "/mensagens", 10L).header(AgenteController.HEADER_USUARIO, 2L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mensagem\":\"oi\"}"))
                .andExpect(status().isServiceUnavailable());

        when(agenteService.conversar(9L, 10L, "oi")).thenThrow(new AcessoNegadoException("fora"));
        mockMvc.perform(post(BASE + "/mensagens", 10L).header(AgenteController.HEADER_USUARIO, 9L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mensagem\":\"oi\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET acoes, POST aprovacao/rejeicao e GET metas")
    void acoesEMetas() throws Exception {
        AcaoDoAgente aprovada = meta();
        aprovada.marcarExecutada(AGORA);
        AcaoDoAgente rejeitada = AcaoDoAgente.lembreteAoAluno(1L, 10L, 3L, "Carla", "bora", AGORA);
        rejeitada.marcarRejeitada(AGORA);
        when(acoesService.listar(2L, 10L)).thenReturn(List.of(meta()));
        when(acoesService.aprovar(2L, 10L, 7L)).thenReturn(aprovada);
        when(acoesService.rejeitar(1L, 10L, 8L)).thenReturn(rejeitada);
        when(acoesService.metas(2L, 10L)).thenReturn(List.of(new MetaDeEstudo(aprovada, AGORA)));

        mockMvc.perform(get(BASE + "/acoes", 10L).header(AgenteController.HEADER_USUARIO, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].missaoId").value(5));
        mockMvc.perform(post(BASE + "/acoes/{acaoId}/aprovacao", 10L, 7L).header(AgenteController.HEADER_USUARIO, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTADA"));
        mockMvc.perform(post(BASE + "/acoes/{acaoId}/rejeicao", 10L, 8L).header(AgenteController.HEADER_USUARIO, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJEITADA"))
                .andExpect(jsonPath("$.alunoAlvoId").value(3));
        mockMvc.perform(get(BASE + "/metas", 10L).header(AgenteController.HEADER_USUARIO, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].missaoId").value(5))
                .andExpect(jsonPath("$[0].prazo").value("2026-12-01"));
    }
}
