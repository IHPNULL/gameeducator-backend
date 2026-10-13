package org.facens.grupo_6_gameeducator.iot;

import java.time.Clock;
import java.util.List;
import org.facens.grupo_6_gameeducator.eventos.EmissorDeEventos;
import org.facens.grupo_6_gameeducator.eventos.EventoIotRegistrado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF06 - registra eventos de laboratorio de forma idempotente e os encaminha ao ecossistema
 * (outbox -> exchange gameeducator.eventos, routing key iot.evento-registrado).
 */
@Service
public class RegistroDeEventosIot {

    public enum Resultado { ACEITO, DUPLICADO }

    private final EventoIotRepository repository;
    private final EmissorDeEventos eventos;
    private final Clock relogio;

    public RegistroDeEventosIot(EventoIotRepository repository, EmissorDeEventos eventos) {
        this.repository = repository;
        this.eventos = eventos;
        this.relogio = Clock.systemUTC();
    }

    @Transactional
    public Resultado registrar(EventoDeLaboratorio evento) {
        if (repository.existsById(evento.eventoId())) {
            return Resultado.DUPLICADO;
        }
        repository.save(new EventoIot(evento, relogio.instant()));
        eventos.emitir(new EventoIotRegistrado(evento.eventoId(), evento.laboratorio(), evento.dispositivo(),
                evento.tipo().name(), evento.alunoId(), evento.atividade(), evento.ocorridoEm()));
        return Resultado.ACEITO;
    }

    @Transactional(readOnly = true)
    public List<EventoIot> recentes(String laboratorio) {
        return repository.findTop100ByLaboratorioOrderByOcorridoEmDesc(laboratorio);
    }

    @Transactional(readOnly = true)
    public long total(String laboratorio) {
        return repository.countByLaboratorio(laboratorio);
    }
}
