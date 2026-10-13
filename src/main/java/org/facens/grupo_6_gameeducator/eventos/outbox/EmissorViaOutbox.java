package org.facens.grupo_6_gameeducator.eventos.outbox;

import org.facens.grupo_6_gameeducator.eventos.EmissorDeEventos;
import org.facens.grupo_6_gameeducator.eventos.EventoDeDominio;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Modo assincrono: o evento vira uma linha do outbox na MESMA transacao da operacao de negocio.
 * Ou os dois sao gravados, ou nenhum (sem "dual write" banco + broker).
 */
public class EmissorViaOutbox implements EmissorDeEventos {

    private final OutboxEventoRepository repository;
    private final JsonMapper json;

    public EmissorViaOutbox(OutboxEventoRepository repository, JsonMapper json) {
        this.repository = repository;
        this.json = json;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitir(EventoDeDominio evento) {
        repository.save(new OutboxEvento(evento.eventoId(), evento.tipo(), json.writeValueAsString(evento), evento.ocorridoEm()));
    }
}
