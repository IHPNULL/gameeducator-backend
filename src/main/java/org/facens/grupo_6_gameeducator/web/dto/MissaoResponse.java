package org.facens.grupo_6_gameeducator.web.dto;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Missao;

public record MissaoResponse(
        Long id,
        String titulo,
        String descricao,
        int xpTotal,
        List<DesafioResponse> desafios
) {

    public static MissaoResponse de(Missao missao) {
        return new MissaoResponse(
                missao.getId(),
                missao.getTitulo(),
                missao.getDescricao(),
                missao.getXpTotal(),
                missao.getDesafios().stream().map(DesafioResponse::de).toList());
    }
}
