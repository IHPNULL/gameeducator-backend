package org.facens.grupo_6_gameeducator.service.agente;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.facens.grupo_6_gameeducator.domain.AcaoDoAgente;
import org.facens.grupo_6_gameeducator.exception.RegraDeNegocioException;
import org.facens.grupo_6_gameeducator.gamificacao.GamificacaoApi;
import org.facens.grupo_6_gameeducator.gamificacao.PosicaoNoRanking;
import org.facens.grupo_6_gameeducator.service.JogoService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Ferramentas do agente de ACOMPANHAMENTO (professor responsavel pelo curso). Presas ao professor e ao curso
 * da requisicao (RNF03). Leitura da turma roda direto; o lembrete ao aluno so vira proposta pendente.
 */
public class FerramentasDeAcompanhamento implements FerramentasDoAgente {

    private final Long professorId;
    private final Long cursoId;
    private final JogoService jogo;
    private final GamificacaoApi gamificacao;
    private final AcoesDoAgenteService acoes;
    private boolean propostaTentada;
    private boolean propostaRegistrada;

    FerramentasDeAcompanhamento(Long professorId, Long cursoId, JogoService jogo, GamificacaoApi gamificacao,
                                AcoesDoAgenteService acoes) {
        this.professorId = professorId;
        this.cursoId = cursoId;
        this.jogo = jogo;
        this.gamificacao = gamificacao;
        this.acoes = acoes;
    }

    public record DesempenhoNaTurma(Long alunoId, String nome, int xpTotal, int tentativas, int acertos) {
    }

    @Tool(description = "Lista o desempenho de cada aluno da turma: id, nome, XP total, tentativas e acertos.")
    public List<DesempenhoNaTurma> consultarDesempenhoDaTurma() {
        return jogo.desempenhoDaTurma(professorId, cursoId).stream()
                .map(d -> new DesempenhoNaTurma(d.aluno().getId(), d.aluno().getNome(), d.xpTotal(), d.tentativas(),
                        d.acertos()))
                .toList();
    }

    @Tool(description = "Consulta o ranking da turma por XP (posicao, id e nome do aluno, XP).")
    public List<PosicaoNoRanking> consultarRankingDaTurma() {
        return gamificacao.ranking(cursoId);
    }

    @Tool(description = "PROPOE enviar um lembrete a um aluno da turma. Nao envia: o lembrete fica pendente ate o "
            + "professor aprovar no painel de acoes.")
    public String proporLembreteAoAluno(@ToolParam(description = "Nome do aluno (ou o id dele)") String aluno,
                                        @ToolParam(description = "Texto do lembrete, curto e encorajador") String mensagem) {
        propostaTentada = true;
        try {
            Long alunoId = resolverAluno(aluno);
            AcaoDoAgente acao = acoes.proporLembrete(professorId, cursoId, alunoId, mensagem);
            propostaRegistrada = true;
            return "Proposta #" + acao.getId() + " registrada e AGUARDANDO APROVACAO do professor: " + acao.getDescricao();
        } catch (RegraDeNegocioException e) {
            return "Nao foi possivel propor o lembrete: " + e.getMessage();
        }
    }

    /**
     * Modelos pequenos erram ids; por isso o aluno pode vir pelo nome. A busca e so na turma deste curso
     * (RNF03): um nome de fora da turma nunca resolve.
     */
    private Long resolverAluno(String aluno) {
        String termo = normalizar(aluno);
        if (termo.isEmpty()) {
            throw new RegraDeNegocioException("Informe o nome do aluno");
        }
        if (termo.chars().allMatch(Character::isDigit)) {
            return Long.valueOf(termo);
        }
        List<DesempenhoNaTurma> encontrados = consultarDesempenhoDaTurma().stream()
                .filter(d -> normalizar(d.nome()).contains(termo))
                .toList();
        if (encontrados.size() != 1) {
            throw new RegraDeNegocioException(encontrados.isEmpty()
                    ? "Nenhum aluno da turma se chama \"" + aluno + "\""
                    : "Mais de um aluno corresponde a \"" + aluno + "\": "
                            + encontrados.stream().map(DesempenhoNaTurma::nome).toList());
        }
        return encontrados.get(0).alunoId();
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).strip();
    }

    @Override
    public boolean propostaTentada() {
        return propostaTentada;
    }

    @Override
    public boolean propostaRegistrada() {
        return propostaRegistrada;
    }
}
