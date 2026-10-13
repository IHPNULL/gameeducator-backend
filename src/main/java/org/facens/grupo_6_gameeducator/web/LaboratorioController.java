package org.facens.grupo_6_gameeducator.web;

import java.time.Instant;
import java.util.List;
import org.facens.grupo_6_gameeducator.iot.EventoIot;
import org.facens.grupo_6_gameeducator.iot.RegistroDeEventosIot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** RF06 - consulta dos eventos de laboratorio recebidos via IoT (evidencia do Desafio 5). */
@RestController
@RequestMapping("/api/laboratorios/{laboratorio}")
public class LaboratorioController {

    private final RegistroDeEventosIot registro;

    public LaboratorioController(RegistroDeEventosIot registro) {
        this.registro = registro;
    }

    public record EventoResponse(String eventoId, String dispositivo, String tipo, Long alunoId, String atividade,
                                 Instant ocorridoEm, Instant recebidoEm) {

        static EventoResponse de(EventoIot e) {
            return new EventoResponse(e.getEventoId(), e.getDispositivo(), e.getTipo().name(), e.getAlunoId(),
                    e.getAtividade(), e.getOcorridoEm(), e.getRecebidoEm());
        }
    }

    public record EventosResponse(String laboratorio, long total, List<EventoResponse> recentes) {
    }

    @GetMapping("/eventos")
    public EventosResponse eventos(@PathVariable String laboratorio) {
        return new EventosResponse(laboratorio, registro.total(laboratorio),
                registro.recentes(laboratorio).stream().map(EventoResponse::de).toList());
    }
}
