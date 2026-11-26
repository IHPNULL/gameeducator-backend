package org.facens.grupo_6_gameeducator.web.dto;

import java.time.Instant;
import java.time.LocalDate;
import org.facens.grupo_6_gameeducator.domain.AcaoDoAgente;
import org.facens.grupo_6_gameeducator.domain.StatusAcaoDoAgente;
import org.facens.grupo_6_gameeducator.domain.TipoAcaoDoAgente;

public record AcaoDoAgenteResponse(Long id, TipoAcaoDoAgente tipo, StatusAcaoDoAgente status, String descricao,
                                   Long missaoId, LocalDate prazo, Long alunoAlvoId, Instant criadaEm,
                                   Instant decididaEm) {

    public static AcaoDoAgenteResponse de(AcaoDoAgente acao) {
        return new AcaoDoAgenteResponse(acao.getId(), acao.getTipo(), acao.getStatus(), acao.getDescricao(),
                acao.getMissaoId(), acao.getPrazo(), acao.getAlunoAlvoId(), acao.getCriadaEm(), acao.getDecididaEm());
    }
}
