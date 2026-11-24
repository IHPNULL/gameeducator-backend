package org.facens.grupo_6_gameeducator.service.agente;

/**
 * O que o Core precisa saber das ferramentas depois que o modelo respondeu: se ele chegou a chamar a
 * ferramenta de proposta. Modelos pequenos as vezes dizem "proposta criada" sem chamar nada; o
 * {@link AgenteService} usa isto para nao repassar essa resposta ao usuario.
 */
interface FerramentasDoAgente {

    /** O modelo chamou a ferramenta de proposta (com ou sem sucesso). */
    boolean propostaTentada();

    /** A ferramenta de proposta registrou de fato uma acao pendente nesta conversa. */
    boolean propostaRegistrada();
}
