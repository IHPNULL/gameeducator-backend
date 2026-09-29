package org.facens.grupo_6_gameeducator.service.dto;

import java.util.List;

/**
 * Retorno de uma resposta enviada pelo aluno.
 *
 * @param correta se a alternativa escolhida e a correta
 * @param xpGanho XP creditado nesta resposta (0 quando erra ou quando ja tinha acertado antes)
 * @param xpTotalNoCurso XP acumulado do aluno no curso apos esta resposta
 * @param medalhasConquistadas marcos de XP cruzados nesta resposta (vazio quando nenhum marco novo foi atingido)
 */
public record ResultadoResposta(
        boolean correta,
        int xpGanho,
        int xpTotalNoCurso,
        List<Integer> medalhasConquistadas
) {

    public ResultadoResposta(boolean correta, int xpGanho, int xpTotalNoCurso) {
        this(correta, xpGanho, xpTotalNoCurso, List.of());
    }
}
