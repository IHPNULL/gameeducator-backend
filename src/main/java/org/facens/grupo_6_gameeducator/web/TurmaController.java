package org.facens.grupo_6_gameeducator.web;

import java.util.List;
import org.facens.grupo_6_gameeducator.service.JogoService;
import org.facens.grupo_6_gameeducator.web.dto.DesempenhoAlunoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Painel do professor: acompanhar o desempenho da turma nas missoes de um curso. */
@RestController
@RequestMapping("/api/cursos/{cursoId}/turma")
public class TurmaController {

    public static final String HEADER_USUARIO = MissaoController.HEADER_USUARIO;

    private final JogoService jogoService;

    public TurmaController(JogoService jogoService) {
        this.jogoService = jogoService;
    }

    /** XP, tentativas e acertos de cada aluno matriculado no curso. So o professor responsavel ve. */
    @GetMapping("/desempenho")
    public List<DesempenhoAlunoResponse> desempenho(@PathVariable Long cursoId,
                                                     @RequestHeader(HEADER_USUARIO) Long usuarioId) {
        return jogoService.desempenhoDaTurma(usuarioId, cursoId).stream()
                .map(DesempenhoAlunoResponse::de)
                .toList();
    }
}
