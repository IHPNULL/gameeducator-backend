package org.facens.grupo_6_gameeducator.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record LancarNotaRequest(
        @NotBlank(message = "informe a avaliacao") String avaliacao,
        @NotNull(message = "informe a nota")
        @DecimalMin(value = "0", message = "a nota precisa estar entre 0 e 10")
        @DecimalMax(value = "10", message = "a nota precisa estar entre 0 e 10")
        BigDecimal valor
) {
}
