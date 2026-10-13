package org.facens.grupo_6_gameeducator.exception;

/** Uma capacidade nao essencial (ex.: assistente de IA) esta indisponivel: mapeada para 503. */
public class ServicoIndisponivelException extends RuntimeException {

    public ServicoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
