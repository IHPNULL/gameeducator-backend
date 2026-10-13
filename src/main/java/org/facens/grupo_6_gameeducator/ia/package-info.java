/**
 * Fronteira do Core com o servico de IA em Python (B1 - Desafio 3, ADR-B1-003).
 *
 * <p>O pacote raiz e a porta de saida ({@link ServicoDeIa}) e o contrato de dados; {@code adaptador}
 * tem o cliente HTTP e a protecao (timeout + disjuntor + metricas). Os services do Core dependem so
 * da porta e decidem o comportamento degradado quando a IA nao responde.
 */
package org.facens.grupo_6_gameeducator.ia;
