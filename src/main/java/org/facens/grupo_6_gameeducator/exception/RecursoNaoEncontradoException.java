package org.facens.grupo_6_gameeducator.exception;

public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public static RecursoNaoEncontradoException de(String recurso, Long id) {
        return new RecursoNaoEncontradoException(recurso + " nao encontrado(a): id=" + id);
    }
}
