package org.facens.grupo_6_gameeducator.eventos.adaptador;

import org.facens.grupo_6_gameeducator.eventos.PublicadorDeEventos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Publicador usado quando nao ha broker (dev/H2): so registra no log. */
public class PublicadorEmLog implements PublicadorDeEventos {

    private static final Logger log = LoggerFactory.getLogger(PublicadorEmLog.class);

    @Override
    public void publicar(String eventoId, String tipo, String payloadJson) {
        log.info("[evento] {} {} {}", tipo, eventoId, payloadJson);
    }
}
