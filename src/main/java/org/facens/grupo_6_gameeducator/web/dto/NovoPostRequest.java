package org.facens.grupo_6_gameeducator.web.dto;

import jakarta.validation.constraints.NotBlank;

public record NovoPostRequest(
        @NotBlank(message = "o post precisa de um titulo") String titulo,
        @NotBlank(message = "o post precisa de um conteudo") String conteudo
) {
}
