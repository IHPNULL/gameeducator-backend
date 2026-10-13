package org.facens.grupo_6_gameeducator.eventos;

/** Porta de saida do relay: entrega um evento ja serializado ao broker. Lanca excecao se o broker nao confirmar. */
public interface PublicadorDeEventos {

    void publicar(String eventoId, String tipo, String payloadJson);
}
