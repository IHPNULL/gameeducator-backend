package org.facens.grupo_6_gameeducator.web.dto;

import java.math.BigDecimal;
import org.facens.grupo_6_gameeducator.domain.Nota;

public record NotaResponse(Long id, String avaliacao, BigDecimal valor) {

    public static NotaResponse de(Nota nota) {
        return new NotaResponse(nota.getId(), nota.getAvaliacao(), nota.getValor());
    }
}
