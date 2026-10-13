package org.facens.grupo_6_gameeducator.web.dto;

import org.facens.grupo_6_gameeducator.gamificacao.PosicaoNoRanking;

public record RankingItemResponse(int posicao, Long alunoId, String nomeAluno, int xpTotal) {

    public static RankingItemResponse de(PosicaoNoRanking posicao) {
        return new RankingItemResponse(posicao.posicao(), posicao.alunoId(), posicao.nomeAluno(), posicao.xpTotal());
    }
}
