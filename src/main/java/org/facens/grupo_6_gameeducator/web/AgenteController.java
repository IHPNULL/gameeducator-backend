package org.facens.grupo_6_gameeducator.web;

import jakarta.validation.Valid;
import java.util.List;
import org.facens.grupo_6_gameeducator.service.agente.AcoesDoAgenteService;
import org.facens.grupo_6_gameeducator.service.agente.AgenteService;
import org.facens.grupo_6_gameeducator.web.dto.AcaoDoAgenteResponse;
import org.facens.grupo_6_gameeducator.web.dto.MensagemAoAgenteRequest;
import org.facens.grupo_6_gameeducator.web.dto.MetaDeEstudoResponse;
import org.facens.grupo_6_gameeducator.web.dto.RespostaDoAgenteResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RF04 - agentes de IA com autorizacao do usuario.
 * <ul>
 *   <li>POST /mensagens: conversa com o agente do seu papel (orientacao ou acompanhamento);</li>
 *   <li>GET /acoes: propostas do agente (pendentes e decididas);</li>
 *   <li>POST /acoes/{id}/aprovacao | /rejeicao: o usuario decide; so a aprovacao executa a acao;</li>
 *   <li>GET /metas: metas de estudo aprovadas pelo aluno.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/cursos/{cursoId}/agente")
public class AgenteController {

    public static final String HEADER_USUARIO = MissaoController.HEADER_USUARIO;

    private final AgenteService agenteService;
    private final AcoesDoAgenteService acoesService;

    public AgenteController(AgenteService agenteService, AcoesDoAgenteService acoesService) {
        this.agenteService = agenteService;
        this.acoesService = acoesService;
    }

    /** 503 se o LLM estiver fora; 403 se o usuario nao participa do curso. */
    @PostMapping("/mensagens")
    public RespostaDoAgenteResponse conversar(@PathVariable Long cursoId,
                                              @RequestHeader(HEADER_USUARIO) Long usuarioId,
                                              @Valid @RequestBody MensagemAoAgenteRequest request) {
        return RespostaDoAgenteResponse.de(agenteService.conversar(usuarioId, cursoId, request.mensagem()));
    }

    @GetMapping("/acoes")
    public List<AcaoDoAgenteResponse> acoes(@PathVariable Long cursoId, @RequestHeader(HEADER_USUARIO) Long usuarioId) {
        return acoesService.listar(usuarioId, cursoId).stream().map(AcaoDoAgenteResponse::de).toList();
    }

    @PostMapping("/acoes/{acaoId}/aprovacao")
    public AcaoDoAgenteResponse aprovar(@PathVariable Long cursoId, @PathVariable Long acaoId,
                                        @RequestHeader(HEADER_USUARIO) Long usuarioId) {
        return AcaoDoAgenteResponse.de(acoesService.aprovar(usuarioId, cursoId, acaoId));
    }

    @PostMapping("/acoes/{acaoId}/rejeicao")
    public AcaoDoAgenteResponse rejeitar(@PathVariable Long cursoId, @PathVariable Long acaoId,
                                         @RequestHeader(HEADER_USUARIO) Long usuarioId) {
        return AcaoDoAgenteResponse.de(acoesService.rejeitar(usuarioId, cursoId, acaoId));
    }

    @GetMapping("/metas")
    public List<MetaDeEstudoResponse> metas(@PathVariable Long cursoId, @RequestHeader(HEADER_USUARIO) Long usuarioId) {
        return acoesService.metas(usuarioId, cursoId).stream().map(MetaDeEstudoResponse::de).toList();
    }
}
