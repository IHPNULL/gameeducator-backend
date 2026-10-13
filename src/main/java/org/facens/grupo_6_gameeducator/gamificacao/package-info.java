/**
 * Modulo de gamificacao do Core (B1 - Desafios 1 e 2): XP, medalhas e ranking.
 *
 * <p>Fronteira publica: somente os tipos deste pacote raiz ({@link GamificacaoApi} e os records
 * de entrada/saida). Os subpacotes sao internos ao modulo:
 * <ul>
 *   <li>{@code dominio} - regras puras (sem Spring, sem JPA), alteradas com frequencia;</li>
 *   <li>{@code porta} - portas de saida que o dominio/aplicacao exigem da infraestrutura;</li>
 *   <li>{@code aplicacao} - caso de uso que orquestra dominio + portas;</li>
 *   <li>{@code adaptador.jpa} / {@code adaptador.memoria} - implementacoes intercambiaveis das portas;</li>
 *   <li>{@code config} - montagem do modulo no Spring.</li>
 * </ul>
 * As regras de dependencia sao verificadas por {@code ArquiteturaModularTest} (ADR-B1-001 e ADR-B1-002).
 */
package org.facens.grupo_6_gameeducator.gamificacao;
