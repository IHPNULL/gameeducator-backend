package org.facens.grupo_6_gameeducator.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MensagemAoAgenteRequest(@NotBlank @Size(max = 1000) String mensagem) {
}
