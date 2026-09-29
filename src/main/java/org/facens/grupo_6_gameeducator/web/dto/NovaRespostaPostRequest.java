package org.facens.grupo_6_gameeducator.web.dto;

import jakarta.validation.constraints.NotBlank;

public record NovaRespostaPostRequest(
        @NotBlank(message = "a resposta precisa de um conteudo") String conteudo
) {
}
