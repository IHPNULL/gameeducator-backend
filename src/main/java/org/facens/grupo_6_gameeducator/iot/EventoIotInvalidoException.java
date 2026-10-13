package org.facens.grupo_6_gameeducator.iot;

/** Payload/topico do dispositivo fora do contrato: a mensagem vai para a DLQ, nao para o dominio. */
public class EventoIotInvalidoException extends RuntimeException {

    public EventoIotInvalidoException(String mensagem) {
        super(mensagem);
    }
}
