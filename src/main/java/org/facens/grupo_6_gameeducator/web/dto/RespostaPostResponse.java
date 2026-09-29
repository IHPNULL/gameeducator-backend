package org.facens.grupo_6_gameeducator.web.dto;

import java.time.LocalDateTime;
import org.facens.grupo_6_gameeducator.domain.RespostaPost;

public record RespostaPostResponse(Long id, String conteudo, String autorNome, LocalDateTime dataHora) {

    public static RespostaPostResponse de(RespostaPost resposta) {
        return new RespostaPostResponse(
                resposta.getId(),
                resposta.getConteudo(),
                resposta.getAutor().getNome(),
                resposta.getDataHora());
    }
}
