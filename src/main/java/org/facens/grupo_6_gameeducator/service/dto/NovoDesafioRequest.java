package org.facens.grupo_6_gameeducator.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Dados de um desafio informados pelo professor no formulario de nova missao.
 *
 * @param indiceRespostaCorreta indice (base 0) da alternativa correta
 * @param xp XP creditado ao aluno no primeiro acerto
 */
public record NovoDesafioRequest(
        @NotBlank(message = "nao pode estar em branco") String enunciado,
        @NotEmpty @Size(min = 2, message = "precisa de ao menos duas alternativas") List<String> alternativas,
        int indiceRespostaCorreta,
        @Positive(message = "precisa ser maior que zero") int xp
) {
}
