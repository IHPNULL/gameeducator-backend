package org.facens.grupo_6_gameeducator.web;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Missao;
import org.facens.grupo_6_gameeducator.service.MissaoService;
import org.facens.grupo_6_gameeducator.service.dto.NovaMissaoRequest;
import org.facens.grupo_6_gameeducator.web.dto.MissaoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * US02 - endpoints de missoes de um curso.
 *
 * <p>O usuario autenticado chega pelo header {@value #HEADER_USUARIO}. Enquanto nao ha
 * Spring Security no projeto, esse header faz o papel do "estou autenticado como..." do BDD.
 */
@RestController
@RequestMapping("/api/cursos/{cursoId}/missoes")
public class MissaoController {

    public static final String HEADER_USUARIO = "X-Usuario-Id";

    private final MissaoService missaoService;

    public MissaoController(MissaoService missaoService) {
        this.missaoService = missaoService;
    }

    /** Professor responsavel cria uma missao com seus desafios. */
    @PostMapping
    public ResponseEntity<MissaoResponse> criar(@PathVariable Long cursoId,
                                                @RequestHeader(HEADER_USUARIO) Long usuarioId,
                                                @Valid @RequestBody NovaMissaoRequest request) {
        Missao missao = missaoService.criar(usuarioId, cursoId, request);
        URI local = URI.create("/api/cursos/" + cursoId + "/missoes/" + missao.getId());
        return ResponseEntity.created(local).body(MissaoResponse.de(missao));
    }

    /** Missoes do curso: aluno matriculado ou professor responsavel. */
    @GetMapping
    public List<MissaoResponse> listar(@PathVariable Long cursoId,
                                       @RequestHeader(HEADER_USUARIO) Long usuarioId) {
        return missaoService.listarParaUsuario(usuarioId, cursoId).stream()
                .map(MissaoResponse::de)
                .toList();
    }
}
