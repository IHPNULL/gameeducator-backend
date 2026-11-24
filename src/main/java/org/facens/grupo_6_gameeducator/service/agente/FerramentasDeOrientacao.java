package org.facens.grupo_6_gameeducator.service.agente;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.AcaoDoAgente;
import org.facens.grupo_6_gameeducator.exception.RegraDeNegocioException;
import org.facens.grupo_6_gameeducator.exception.ServicoIndisponivelException;
import org.facens.grupo_6_gameeducator.gamificacao.GamificacaoApi;
import org.facens.grupo_6_gameeducator.gamificacao.MedalhaDoAluno;
import org.facens.grupo_6_gameeducator.gamificacao.PosicaoNoRanking;
import org.facens.grupo_6_gameeducator.service.AssistenteService;
import org.facens.grupo_6_gameeducator.service.RecomendacaoService;
import org.facens.grupo_6_gameeducator.service.dto.Recomendacoes;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Ferramentas do agente de ORIENTACAO (aluno). Uma instancia por requisicao, presa ao aluno e ao curso de
 * quem conversa: nenhuma ferramenta recebe "alunoId" do modelo, entao o LLM nao consegue ler dados de outra
 * pessoa (RNF03). Leitura roda direto; a unica acao (meta de estudo) so vira proposta pendente.
 */
public class FerramentasDeOrientacao {

    private final Long alunoId;
    private final Long cursoId;
    private final GamificacaoApi gamificacao;
    private final RecomendacaoService recomendacoes;
    private final AssistenteService assistente;
    private final AcoesDoAgenteService acoes;

    FerramentasDeOrientacao(Long alunoId, Long cursoId, GamificacaoApi gamificacao, RecomendacaoService recomendacoes,
                            AssistenteService assistente, AcoesDoAgenteService acoes) {
        this.alunoId = alunoId;
        this.cursoId = cursoId;
        this.gamificacao = gamificacao;
        this.recomendacoes = recomendacoes;
        this.assistente = assistente;
        this.acoes = acoes;
    }

    public record Progresso(int xpTotal, List<Integer> medalhas, Integer posicaoNoRanking, int alunosNoRanking) {
    }

    @Tool(description = "Consulta o progresso do aluno neste curso: XP total, medalhas (marcos de XP) e posicao no ranking da turma.")
    public Progresso consultarMeuProgresso() {
        List<PosicaoNoRanking> ranking = gamificacao.ranking(cursoId);
        Integer posicao = ranking.stream()
                .filter(p -> p.alunoId().equals(alunoId))
                .map(PosicaoNoRanking::posicao)
                .findFirst()
                .orElse(null);
        List<Integer> medalhas = gamificacao.medalhasDoAluno(alunoId, cursoId).stream()
                .map(MedalhaDoAluno::marcoXp)
                .toList();
        return new Progresso(gamificacao.xpNoCurso(alunoId, cursoId), medalhas, posicao, ranking.size());
    }

    @Tool(description = "Lista as missoes do curso que o aluno deveria estudar agora, com id, titulo e o motivo da recomendacao.")
    public List<Recomendacoes.Item> recomendarMissoes() {
        return recomendacoes.recomendar(alunoId, cursoId).itens();
    }

    @Tool(description = "Busca no material das missoes do curso (RAG) a resposta para uma duvida de conteudo do aluno.")
    public String consultarMaterialDoCurso(@ToolParam(description = "A duvida do aluno, em portugues") String pergunta) {
        try {
            return assistente.perguntar(alunoId, cursoId, pergunta).resposta();
        } catch (ServicoIndisponivelException e) {
            return "O material do curso esta indisponivel no momento.";
        }
    }

    @Tool(description = "PROPOE uma meta de estudo para o aluno concluir uma missao do curso dentro de um prazo. "
            + "Nao cria a meta: ela fica pendente ate o aluno aprovar no painel de acoes. "
            + "Use somente ids de missao obtidos com recomendarMissoes.")
    public String proporMetaDeEstudo(@ToolParam(description = "Id da missao") Long missaoId,
                                     @ToolParam(description = "Prazo em dias a partir de hoje, de 1 a 90") int prazoEmDias,
                                     @ToolParam(description = "Objetivo curto da meta, em portugues") String objetivo) {
        try {
            AcaoDoAgente acao = acoes.proporMetaDeEstudo(alunoId, cursoId, missaoId, prazoEmDias, objetivo);
            return "Proposta #" + acao.getId() + " registrada e AGUARDANDO APROVACAO do aluno: " + acao.getDescricao();
        } catch (RegraDeNegocioException e) {
            return "Nao foi possivel propor a meta: " + e.getMessage();
        }
    }
}
