package org.facens.grupo_6_gameeducator.web.dto;

import org.facens.grupo_6_gameeducator.domain.ProgressoAluno;

public record RankingItemResponse(int posicao, Long alunoId, String nomeAluno, int xpTotal) {

    public static RankingItemResponse de(int posicao, ProgressoAluno progresso) {
        return new RankingItemResponse(
                posicao,
                progresso.getAluno().getId(),
                progresso.getAluno().getNome(),
                progresso.getXpTotal());
    }
}
