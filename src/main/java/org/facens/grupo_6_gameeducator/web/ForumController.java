package org.facens.grupo_6_gameeducator.web;

import jakarta.validation.Valid;
import java.util.List;
import org.facens.grupo_6_gameeducator.service.ForumService;
import org.facens.grupo_6_gameeducator.web.dto.NovaRespostaPostRequest;
import org.facens.grupo_6_gameeducator.web.dto.NovoPostRequest;
import org.facens.grupo_6_gameeducator.web.dto.PostResponse;
import org.facens.grupo_6_gameeducator.web.dto.RespostaPostResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Aba de foruns: aluno matriculado ou professor responsavel leem, publicam e respondem. */
@RestController
@RequestMapping("/api")
public class ForumController {

    public static final String HEADER_USUARIO = MissaoController.HEADER_USUARIO;

    private final ForumService forumService;

    public ForumController(ForumService forumService) {
        this.forumService = forumService;
    }

    /** Posts do forum do curso, do mais recente para o mais antigo. */
    @GetMapping("/cursos/{cursoId}/forum")
    public List<PostResponse> listar(@PathVariable Long cursoId,
                                     @RequestHeader(HEADER_USUARIO) Long usuarioId) {
        return forumService.listarPosts(usuarioId, cursoId).stream()
                .map(PostResponse::de)
                .toList();
    }

    /** Publica um post proprio no forum do curso. */
    @PostMapping("/cursos/{cursoId}/forum")
    public PostResponse publicar(@PathVariable Long cursoId,
                                 @RequestHeader(HEADER_USUARIO) Long usuarioId,
                                 @Valid @RequestBody NovoPostRequest request) {
        return PostResponse.de(forumService.publicar(usuarioId, cursoId, request.titulo(), request.conteudo()));
    }

    /** Responde um post existente do forum. */
    @PostMapping("/forum/posts/{postId}/respostas")
    public RespostaPostResponse responder(@PathVariable Long postId,
                                          @RequestHeader(HEADER_USUARIO) Long usuarioId,
                                          @Valid @RequestBody NovaRespostaPostRequest request) {
        return RespostaPostResponse.de(forumService.responder(usuarioId, postId, request.conteudo()));
    }
}
