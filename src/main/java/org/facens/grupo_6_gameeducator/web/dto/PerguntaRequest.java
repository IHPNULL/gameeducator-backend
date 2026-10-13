package org.facens.grupo_6_gameeducator.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerguntaRequest(@NotBlank @Size(max = 500) String pergunta) {
}
