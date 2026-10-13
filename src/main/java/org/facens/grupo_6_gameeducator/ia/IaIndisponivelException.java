package org.facens.grupo_6_gameeducator.ia;

/** O servico de IA nao respondeu a tempo, recusou a conexao, falhou ou o disjuntor esta aberto. */
public class IaIndisponivelException extends RuntimeException {

    public IaIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
