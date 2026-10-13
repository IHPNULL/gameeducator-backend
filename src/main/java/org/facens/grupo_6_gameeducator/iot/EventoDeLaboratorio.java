package org.facens.grupo_6_gameeducator.iot;

import java.time.Instant;

/** Modelo canonico (interno ao Core) de um evento de laboratorio, ja traduzido do formato do dispositivo. */
public record EventoDeLaboratorio(String eventoId, String laboratorio, String dispositivo, TipoEventoIot tipo,
                                  Long alunoId, String atividade, Instant ocorridoEm) {
}
