package org.facens.grupo_6_gameeducator.iot;

/** Eventos que os dispositivos de laboratorio produzem. Codigo de 1 letra no payload (economia de banda). */
public enum TipoEventoIot {
    PRESENCA("P"), INICIO_ATIVIDADE("I"), CONCLUSAO_ATIVIDADE("C");

    private final String codigo;

    TipoEventoIot(String codigo) {
        this.codigo = codigo;
    }

    public static TipoEventoIot doCodigo(String codigo) {
        for (TipoEventoIot tipo : values()) {
            if (tipo.codigo.equals(codigo)) {
                return tipo;
            }
        }
        throw new EventoIotInvalidoException("Tipo de evento IoT desconhecido: " + codigo);
    }
}
