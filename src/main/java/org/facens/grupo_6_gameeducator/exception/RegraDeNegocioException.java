package org.facens.grupo_6_gameeducator.exception;

/** Dados validos sintaticamente, mas que violam uma regra do dominio. */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
