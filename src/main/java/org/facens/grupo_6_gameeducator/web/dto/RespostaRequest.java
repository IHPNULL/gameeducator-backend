package org.facens.grupo_6_gameeducator.web.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Corpo do POST em que o aluno envia sua resposta.
 *
 * @param indiceResposta indice (base 0) da alternativa escolhida
 */
public record RespostaRequest(
        @NotNull(message = "informe o indice da alternativa escolhida") Integer indiceResposta
) {
}
