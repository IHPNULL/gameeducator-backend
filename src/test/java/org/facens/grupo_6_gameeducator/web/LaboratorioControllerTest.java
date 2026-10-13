package org.facens.grupo_6_gameeducator.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.facens.grupo_6_gameeducator.iot.EventoDeLaboratorio;
import org.facens.grupo_6_gameeducator.iot.EventoIot;
import org.facens.grupo_6_gameeducator.iot.RegistroDeEventosIot;
import org.facens.grupo_6_gameeducator.iot.TipoEventoIot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LaboratorioController.class)
@DisplayName("Endpoint de eventos de laboratorio")
class LaboratorioControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private RegistroDeEventosIot registro;

    @Test
    @DisplayName("GET eventos -> total e eventos recentes do laboratorio")
    void eventos() throws Exception {
        EventoIot evento = new EventoIot(new EventoDeLaboratorio("x", "lab-01", "porta-01", TipoEventoIot.PRESENCA, 42L,
                null, Instant.parse("2026-10-01T10:00:00Z")), Instant.parse("2026-10-01T10:00:01Z"));
        when(registro.total("lab-01")).thenReturn(1L);
        when(registro.recentes("lab-01")).thenReturn(List.of(evento));

        mockMvc.perform(get("/api/laboratorios/{lab}/eventos", "lab-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.recentes[0].tipo").value("PRESENCA"))
                .andExpect(jsonPath("$.recentes[0].dispositivo").value("porta-01"));
    }
}
