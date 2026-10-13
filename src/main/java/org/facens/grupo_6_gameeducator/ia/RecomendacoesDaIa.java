package org.facens.grupo_6_gameeducator.ia;

import java.util.List;

public record RecomendacoesDaIa(String modeloVersao, List<Item> itens) {

    public record Item(Long missaoId, String titulo, double pontuacao, String motivo) {
    }
}
