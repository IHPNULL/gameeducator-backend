package org.facens.grupo_6_gameeducator.ia;

import java.util.List;

public record RespostaDoAssistente(String resposta, List<Fonte> fontes, String provedor) {

    public record Fonte(String id, String titulo, double relevancia) {
    }
}
