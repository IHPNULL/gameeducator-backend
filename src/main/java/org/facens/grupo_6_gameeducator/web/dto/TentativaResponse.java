package org.facens.grupo_6_gameeducator.web.dto;

import java.time.LocalDateTime;
import org.facens.grupo_6_gameeducator.domain.Tentativa;

public record TentativaResponse(
        Long id,
        int indiceResposta,
        boolean correta,
        int xpGanho,
        LocalDateTime dataHora
) {

    public static TentativaResponse de(Tentativa tentativa) {
        return new TentativaResponse(
                tentativa.getId(),
                tentativa.getIndiceResposta(),
                tentativa.isCorreta(),
                tentativa.getXpGanho(),
                tentativa.getDataHora());
    }
}
