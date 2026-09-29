package org.facens.grupo_6_gameeducator.exception;

/** Usuario autenticado, mas sem permissao para a operacao (ex.: professor que nao e o responsavel pelo curso). */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
