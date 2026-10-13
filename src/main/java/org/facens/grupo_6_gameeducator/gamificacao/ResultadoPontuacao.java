package org.facens.grupo_6_gameeducator.gamificacao;

import java.util.List;

/**
 * Efeito de uma resposta na gamificacao.
 *
 * @param xpGanho XP creditado nesta resposta
 * @param xpTotalNoCurso XP acumulado apos a resposta
 * @param medalhasConquistadas marcos de XP cruzados nesta resposta
 */
public record ResultadoPontuacao(int xpGanho, int xpTotalNoCurso, List<Integer> medalhasConquistadas) {

    public ResultadoPontuacao {
        medalhasConquistadas = List.copyOf(medalhasConquistadas);
    }
}
