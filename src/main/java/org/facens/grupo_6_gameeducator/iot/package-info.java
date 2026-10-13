/**
 * Integracao IoT dos laboratorios (B1 - Desafio 5, ADR-B1-005, RF06).
 *
 * <p>Dispositivos publicam MQTT (QoS 1) no MESMO RabbitMQ do Desafio 4 (plugin MQTT): nao ha segundo broker.
 * O Core nunca fala MQTT nem conhece cada dispositivo: consome uma fila AMQP com eventos ja roteados e passa
 * por uma camada anticorrupcao ({@link TradutorDeEventoIot}) que valida e converte o payload compacto do
 * dispositivo para o modelo canonico. Duplicidade (reentrega QoS 1, reenvio apos reconexao) e neutralizada
 * por idempotencia no {@code eventoId} gerado pelo dispositivo.
 */
package org.facens.grupo_6_gameeducator.iot;
