package org.facens.grupo_6_gameeducator.web.dto;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Desafio;

/**
 * Desafio como ele vai para o cliente.
 * O indice da resposta correta NAO e exposto: o aluno descobre acertando.
 */
public record DesafioResponse(
        Long id,
        String enunciado,
        List<String> alternativas,
        int xp
) {

    public static DesafioResponse de(Desafio desafio) {
        return new DesafioResponse(
                desafio.getId(),
                desafio.getEnunciado(),
                List.copyOf(desafio.getAlternativas()),
                desafio.getXp());
    }
}
