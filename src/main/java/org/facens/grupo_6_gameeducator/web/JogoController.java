package org.facens.grupo_6_gameeducator.web;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.IntStream;
import org.facens.grupo_6_gameeducator.domain.ProgressoAluno;
import org.facens.grupo_6_gameeducator.service.JogoService;
import org.facens.grupo_6_gameeducator.service.dto.ResultadoResposta;
import org.facens.grupo_6_gameeducator.web.dto.MedalhaResponse;
import org.facens.grupo_6_gameeducator.web.dto.ProgressoResponse;
import org.facens.grupo_6_gameeducator.web.dto.RankingItemResponse;
import org.facens.grupo_6_gameeducator.web.dto.RespostaRequest;
import org.facens.grupo_6_gameeducator.web.dto.TentativaResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** US01 - endpoints do aluno jogando: responder desafios, ver progresso, historico e ranking. */
@RestController
@RequestMapping("/api")
public class JogoController {

    public static final String HEADER_USUARIO = MissaoController.HEADER_USUARIO;

    private final JogoService jogoService;

    public JogoController(JogoService jogoService) {
        this.jogoService = jogoService;
    }

    /** Aluno matriculado responde um desafio e recebe o resultado com o XP. */
    @PostMapping("/desafios/{desafioId}/respostas")
    public ResultadoResposta responder(@PathVariable Long desafioId,
                                       @RequestHeader(HEADER_USUARIO) Long usuarioId,
                                       @Valid @RequestBody RespostaRequest request) {
        return jogoService.responder(usuarioId, desafioId, request.indiceResposta());
    }

    /** Historico de tentativas do proprio aluno em um desafio. */
    @GetMapping("/desafios/{desafioId}/tentativas")
    public List<TentativaResponse> historico(@PathVariable Long desafioId,
                                             @RequestHeader(HEADER_USUARIO) Long usuarioId) {
        return jogoService.historico(usuarioId, desafioId).stream()
                .map(TentativaResponse::de)
                .toList();
    }

    /** XP acumulado do proprio aluno no curso. */
    @GetMapping("/cursos/{cursoId}/progresso")
    public ProgressoResponse progresso(@PathVariable Long cursoId,
                                       @RequestHeader(HEADER_USUARIO) Long usuarioId) {
        return new ProgressoResponse(cursoId, usuarioId, jogoService.xpNoCurso(usuarioId, cursoId));
    }

    /** Ranking de XP da turma, do maior para o menor. */
    @GetMapping("/cursos/{cursoId}/ranking")
    public List<RankingItemResponse> ranking(@PathVariable Long cursoId) {
        List<ProgressoAluno> progressos = jogoService.ranking(cursoId);
        return IntStream.range(0, progressos.size())
                .mapToObj(i -> RankingItemResponse.de(i + 1, progressos.get(i)))
                .toList();
    }

    /** Medalhas conquistadas pelo proprio aluno no curso. */
    @GetMapping("/cursos/{cursoId}/medalhas")
    public List<MedalhaResponse> medalhas(@PathVariable Long cursoId,
                                          @RequestHeader(HEADER_USUARIO) Long usuarioId) {
        return jogoService.medalhasDoAluno(usuarioId, cursoId).stream()
                .map(MedalhaResponse::de)
                .toList();
    }
}
