package org.facens.grupo_6_gameeducator.service.dto;

import java.util.List;

/**
 * Recomendacoes de missoes para o aluno.
 *
 * @param origem "ia" quando vieram do servico Python; "fallback" quando a IA estava indisponivel e o
 *               Core aplicou a regra local (proximas missoes nao concluidas, na ordem do curso)
 */
public record Recomendacoes(String origem, String modeloVersao, List<Item> itens) {

    public static final String ORIGEM_IA = "ia";
    public static final String ORIGEM_FALLBACK = "fallback";

    public record Item(Long missaoId, String titulo, double pontuacao, String motivo) {
    }
}
