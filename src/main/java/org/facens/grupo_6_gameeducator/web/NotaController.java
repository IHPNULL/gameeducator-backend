package org.facens.grupo_6_gameeducator.web;

import jakarta.validation.Valid;
import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Nota;
import org.facens.grupo_6_gameeducator.service.NotaService;
import org.facens.grupo_6_gameeducator.web.dto.LancarNotaRequest;
import org.facens.grupo_6_gameeducator.web.dto.NotaResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Aba de notas: aluno ve as proprias, professor responsavel lanca. */
@RestController
@RequestMapping("/api/cursos/{cursoId}/notas")
public class NotaController {

    public static final String HEADER_USUARIO = MissaoController.HEADER_USUARIO;

    private final NotaService notaService;

    public NotaController(NotaService notaService) {
        this.notaService = notaService;
    }

    /** Notas do proprio aluno naquele curso. */
    @GetMapping
    public List<NotaResponse> minhasNotas(@PathVariable Long cursoId,
                                          @RequestHeader(HEADER_USUARIO) Long usuarioId) {
        return notaService.minhasNotas(usuarioId, cursoId).stream()
                .map(NotaResponse::de)
                .toList();
    }

    /** Professor responsavel lanca (ou atualiza) a nota de um aluno matriculado. */
    @PostMapping("/{alunoId}")
    public NotaResponse lancar(@PathVariable Long cursoId,
                               @PathVariable Long alunoId,
                               @RequestHeader(HEADER_USUARIO) Long usuarioId,
                               @Valid @RequestBody LancarNotaRequest request) {
        Nota nota = notaService.lancar(usuarioId, cursoId, alunoId, request.avaliacao(), request.valor());
        return NotaResponse.de(nota);
    }
}
