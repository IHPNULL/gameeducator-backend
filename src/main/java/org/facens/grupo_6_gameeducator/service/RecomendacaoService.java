package org.facens.grupo_6_gameeducator.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.facens.grupo_6_gameeducator.domain.Desafio;
import org.facens.grupo_6_gameeducator.domain.Missao;
import org.facens.grupo_6_gameeducator.domain.Tentativa;
import org.facens.grupo_6_gameeducator.exception.AcessoNegadoException;
import org.facens.grupo_6_gameeducator.ia.IaIndisponivelException;
import org.facens.grupo_6_gameeducator.ia.PedidoDeRecomendacao;
import org.facens.grupo_6_gameeducator.ia.PedidoDeRecomendacao.MissaoParaRecomendar;
import org.facens.grupo_6_gameeducator.ia.RecomendacoesDaIa;
import org.facens.grupo_6_gameeducator.ia.ServicoDeIa;
import org.facens.grupo_6_gameeducator.repository.MatriculaRepository;
import org.facens.grupo_6_gameeducator.repository.MissaoRepository;
import org.facens.grupo_6_gameeducator.repository.TentativaRepository;
import org.facens.grupo_6_gameeducator.service.dto.Recomendacoes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * RF03 - recomendacao inteligente de missoes (Desafio 3).
 *
 * <p>Comportamento degradado aceito: se a IA falhar, o aluno recebe as proximas missoes nao concluidas
 * (regra local) com origem "fallback" - a recomendacao piora, mas a tela nunca quebra.
 * O contexto e lido do banco em uma transacao curta e a chamada HTTP acontece FORA dela, para nao
 * prender conexao do pool enquanto espera a IA.
 */
@Service
public class RecomendacaoService {

    private static final Logger log = LoggerFactory.getLogger(RecomendacaoService.class);
    private static final int LIMITE = 3;

    private final MatriculaRepository matriculaRepository;
    private final MissaoRepository missaoRepository;
    private final TentativaRepository tentativaRepository;
    private final ServicoDeIa servicoDeIa;
    private final TransactionTemplate leitura;

    public RecomendacaoService(MatriculaRepository matriculaRepository,
                               MissaoRepository missaoRepository,
                               TentativaRepository tentativaRepository,
                               ServicoDeIa servicoDeIa,
                               PlatformTransactionManager transactionManager) {
        this.matriculaRepository = matriculaRepository;
        this.missaoRepository = missaoRepository;
        this.tentativaRepository = tentativaRepository;
        this.servicoDeIa = servicoDeIa;
        this.leitura = new TransactionTemplate(transactionManager);
        this.leitura.setReadOnly(true);
    }

    public Recomendacoes recomendar(Long alunoId, Long cursoId) {
        PedidoDeRecomendacao pedido = leitura.execute(status -> montarPedido(alunoId, cursoId));
        try {
            RecomendacoesDaIa daIa = servicoDeIa.recomendar(pedido);
            return new Recomendacoes(Recomendacoes.ORIGEM_IA, daIa.modeloVersao(), daIa.itens().stream()
                    .map(item -> new Recomendacoes.Item(item.missaoId(), item.titulo(), item.pontuacao(), item.motivo()))
                    .toList());
        } catch (IaIndisponivelException e) {
            log.warn("IA indisponivel, usando recomendacao local: {}", e.getMessage());
            return fallback(pedido);
        }
    }

    private PedidoDeRecomendacao montarPedido(Long alunoId, Long cursoId) {
        if (!matriculaRepository.existsByCursoIdAndAlunoId(cursoId, alunoId)) {
            throw new AcessoNegadoException("Aluno " + alunoId + " nao esta matriculado no curso " + cursoId);
        }
        List<Tentativa> tentativas = tentativaRepository.findByAlunoIdAndDesafio_Missao_Curso_Id(alunoId, cursoId);
        Map<Long, List<Tentativa>> porMissao = tentativas.stream()
                .collect(Collectors.groupingBy(t -> t.getDesafio().getMissao().getId()));

        List<MissaoParaRecomendar> missoes = missaoRepository.findByCursoIdOrderByIdAsc(cursoId).stream()
                .map(missao -> resumo(missao, porMissao.getOrDefault(missao.getId(), List.of())))
                .toList();
        return new PedidoDeRecomendacao(alunoId, cursoId, missoes, LIMITE);
    }

    private static MissaoParaRecomendar resumo(Missao missao, List<Tentativa> tentativas) {
        Set<Long> acertados = tentativas.stream()
                .filter(Tentativa::isCorreta)
                .map(t -> t.getDesafio().getId())
                .collect(Collectors.toSet());
        Map<Long, Desafio> desafios = missao.getDesafios().stream()
                .collect(Collectors.toMap(Desafio::getId, Function.identity()));
        acertados.retainAll(desafios.keySet());
        return new MissaoParaRecomendar(missao.getId(), missao.getTitulo(), desafios.size(), acertados.size(),
                tentativas.size(), missao.getXpTotal());
    }

    private static Recomendacoes fallback(PedidoDeRecomendacao pedido) {
        List<Recomendacoes.Item> itens = pedido.missoes().stream()
                .filter(missao -> !missao.concluida() && missao.totalDesafios() > 0)
                .limit(pedido.limite())
                .map(missao -> new Recomendacoes.Item(missao.missaoId(), missao.titulo(), 0.0,
                        "Recomendacao local (IA indisponivel): proxima missao nao concluida"))
                .toList();
        return new Recomendacoes(Recomendacoes.ORIGEM_FALLBACK, null, itens);
    }
}
