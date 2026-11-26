package org.facens.grupo_6_gameeducator.web.dto;

import java.time.LocalDate;
import org.facens.grupo_6_gameeducator.domain.MetaDeEstudo;

public record MetaDeEstudoResponse(Long id, Long missaoId, String descricao, LocalDate prazo, Long acaoId) {

    public static MetaDeEstudoResponse de(MetaDeEstudo meta) {
        return new MetaDeEstudoResponse(meta.getId(), meta.getMissaoId(), meta.getDescricao(), meta.getPrazo(),
                meta.getAcaoId());
    }
}
