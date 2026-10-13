package org.facens.grupo_6_gameeducator.eventos;

/**
 * Porta usada pelos services para emitir eventos. Implementacoes (app.eventos.modo):
 * <ul>
 *   <li>{@code assincrono} (padrao): grava no outbox na transacao do chamador;</li>
 *   <li>{@code sincrono} (so para o lab do Desafio 4): chama o servico de notificacao via HTTP dentro da
 *       requisicao - demonstra o acoplamento temporal que a decisao evita.</li>
 * </ul>
 */
public interface EmissorDeEventos {

    void emitir(EventoDeDominio evento);
}
