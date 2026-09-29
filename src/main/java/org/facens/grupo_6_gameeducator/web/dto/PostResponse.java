package org.facens.grupo_6_gameeducator.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Post;

public record PostResponse(
        Long id,
        String titulo,
        String conteudo,
        String autorNome,
        LocalDateTime dataHora,
        List<RespostaPostResponse> respostas
) {

    public static PostResponse de(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitulo(),
                post.getConteudo(),
                post.getAutor().getNome(),
                post.getDataHora(),
                post.getRespostas().stream().map(RespostaPostResponse::de).toList());
    }
}
