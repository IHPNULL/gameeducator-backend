package org.facens.grupo_6_gameeducator.service;

import java.util.List;
import java.util.stream.Collectors;
import org.facens.grupo_6_gameeducator.domain.Desafio;
import org.facens.grupo_6_gameeducator.domain.Missao;
import org.facens.grupo_6_gameeducator.exception.AcessoNegadoException;
import org.facens.grupo_6_gameeducator.exception.ServicoIndisponivelException;
import org.facens.grupo_6_gameeducator.ia.IaIndisponivelException;
import org.facens.grupo_6_gameeducator.ia.PerguntaAoAssistente;
import org.facens.grupo_6_gameeducator.ia.RespostaDoAssistente;
import org.facens.grupo_6_gameeducator.ia.ServicoDeIa;
import org.facens.grupo_6_gameeducator.repository.MatriculaRepository;
import org.facens.grupo_6_gameeducator.repository.MissaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * RF04 - assistente educacional (RAG) (Desafio 3).
 *
 * <p>Comportamento degradado aceito: sem IA nao ha resposta "inventada" pelo Core. O assistente devolve
 * 503 com mensagem clara e todas as outras funcoes (missoes, XP, ranking) seguem funcionando.
 */
@Service
public class AssistenteService {

    static final String MENSAGEM_INDISPONIVEL =
            "Assistente educacional indisponivel no momento. O restante da plataforma segue funcionando.";

    private final MatriculaRepository matriculaRepository;
    private final MissaoRepository missaoRepository;
    private final ServicoDeIa servicoDeIa;
    private final TransactionTemplate leitura;

    public AssistenteService(MatriculaRepository matriculaRepository,
                             MissaoRepository missaoRepository,
                             ServicoDeIa servicoDeIa,
                             PlatformTransactionManager transactionManager) {
        this.matriculaRepository = matriculaRepository;
        this.missaoRepository = missaoRepository;
        this.servicoDeIa = servicoDeIa;
        this.leitura = new TransactionTemplate(transactionManager);
        this.leitura.setReadOnly(true);
    }

    public RespostaDoAssistente perguntar(Long alunoId, Long cursoId, String pergunta) {
        PerguntaAoAssistente pedido = leitura.execute(status -> montarPedido(alunoId, cursoId, pergunta));
        try {
            return servicoDeIa.perguntar(pedido);
        } catch (IaIndisponivelException e) {
            throw new ServicoIndisponivelException(MENSAGEM_INDISPONIVEL);
        }
    }

    private PerguntaAoAssistente montarPedido(Long alunoId, Long cursoId, String pergunta) {
        if (!matriculaRepository.existsByCursoIdAndAlunoId(cursoId, alunoId)) {
            throw new AcessoNegadoException("Aluno " + alunoId + " nao esta matriculado no curso " + cursoId);
        }
        List<PerguntaAoAssistente.Documento> documentos = missaoRepository.findByCursoIdOrderByIdAsc(cursoId).stream()
                .map(AssistenteService::documento)
                .toList();
        return new PerguntaAoAssistente(cursoId, pergunta, documentos);
    }

    private static PerguntaAoAssistente.Documento documento(Missao missao) {
        String enunciados = missao.getDesafios().stream()
                .map(Desafio::getEnunciado)
                .collect(Collectors.joining(". "));
        String descricao = missao.getDescricao() == null ? "" : missao.getDescricao() + ". ";
        return new PerguntaAoAssistente.Documento("missao-" + missao.getId(), missao.getTitulo(), descricao + enunciados);
    }
}
