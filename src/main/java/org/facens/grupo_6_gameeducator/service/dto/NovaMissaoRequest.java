package org.facens.grupo_6_gameeducator.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record NovaMissaoRequest(
        @NotBlank(message = "nao pode estar em branco") String titulo,
        String descricao,
        @NotEmpty(message = "a missao precisa de ao menos um desafio") @Valid List<NovoDesafioRequest> desafios
) {
}
