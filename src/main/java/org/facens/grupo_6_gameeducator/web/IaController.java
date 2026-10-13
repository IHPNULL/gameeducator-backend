package org.facens.grupo_6_gameeducator.web;

import jakarta.validation.Valid;
import org.facens.grupo_6_gameeducator.ia.RespostaDoAssistente;
import org.facens.grupo_6_gameeducator.service.AssistenteService;
import org.facens.grupo_6_gameeducator.service.RecomendacaoService;
import org.facens.grupo_6_gameeducator.service.dto.Recomendacoes;
import org.facens.grupo_6_gameeducator.web.dto.PerguntaRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** RF03/RF04 - capacidades de IA expostas pelo Core (a IA em si roda no servico Python). */
@RestController
@RequestMapping("/api/cursos/{cursoId}")
public class IaController {

    public static final String HEADER_USUARIO = MissaoController.HEADER_USUARIO;

    private final RecomendacaoService recomendacaoService;
    private final AssistenteService assistenteService;

    public IaController(RecomendacaoService recomendacaoService, AssistenteService assistenteService) {
        this.recomendacaoService = recomendacaoService;
        this.assistenteService = assistenteService;
    }

    /** Proximas missoes recomendadas ao aluno. Sempre 200: com IA fora, origem = "fallback". */
    @GetMapping("/recomendacoes")
    public Recomendacoes recomendacoes(@PathVariable Long cursoId, @RequestHeader(HEADER_USUARIO) Long usuarioId) {
        return recomendacaoService.recomendar(usuarioId, cursoId);
    }

    /** Pergunta ao assistente educacional (RAG sobre o material do curso). 503 se a IA estiver fora. */
    @PostMapping("/assistente")
    public RespostaDoAssistente perguntar(@PathVariable Long cursoId,
                                          @RequestHeader(HEADER_USUARIO) Long usuarioId,
                                          @Valid @RequestBody PerguntaRequest request) {
        return assistenteService.perguntar(usuarioId, cursoId, request.pergunta());
    }
}
