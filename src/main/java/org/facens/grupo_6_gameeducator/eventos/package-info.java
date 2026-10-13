/**
 * Eventos de dominio do Core (B1 - Desafio 4, ADR-B1-004).
 *
 * <p>O que precisa ser imediato (resposta, XP, medalha, conclusao) e sincrono e transacional. O que pode ser
 * eventual (notificacao, analytics, eventos IoT para o ecossistema) sai como evento: gravado no outbox na
 * MESMA transacao e publicado depois no RabbitMQ pelo relay. Consumidor fora do ar nunca impede a operacao.
 */
package org.facens.grupo_6_gameeducator.eventos;
