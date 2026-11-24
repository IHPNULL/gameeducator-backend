package org.facens.grupo_6_gameeducator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Function;
import org.facens.grupo_6_gameeducator.domain.AcaoDoAgente;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Missao;
import org.facens.grupo_6_gameeducator.domain.StatusAcaoDoAgente;
import org.facens.grupo_6_gameeducator.domain.TipoAcaoDoAgente;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.eventos.LembreteAoAlunoEnviado;
import org.facens.grupo_6_gameeducator.eventos.outbox.OutboxEvento;
import org.facens.grupo_6_gameeducator.eventos.outbox.OutboxEventoRepository;
import org.facens.grupo_6_gameeducator.exception.AcessoNegadoException;
import org.facens.grupo_6_gameeducator.exception.RecursoNaoEncontradoException;
import org.facens.grupo_6_gameeducator.exception.RegraDeNegocioException;
import org.facens.grupo_6_gameeducator.exception.ServicoIndisponivelException;
import org.facens.grupo_6_gameeducator.ia.IaIndisponivelException;
import org.facens.grupo_6_gameeducator.ia.OrquestradorDeAgente;
import org.facens.grupo_6_gameeducator.ia.OrquestradorDeAgente.PedidoAoAgente;
import org.facens.grupo_6_gameeducator.ia.RecomendacoesDaIa;
import org.facens.grupo_6_gameeducator.ia.RespostaDoAssistente;
import org.facens.grupo_6_gameeducator.ia.ServicoDeIa;
import org.facens.grupo_6_gameeducator.repository.MetaDeEstudoRepository;
import org.facens.grupo_6_gameeducator.service.agente.AcoesDoAgenteService;
import org.facens.grupo_6_gameeducator.service.agente.AgenteService;
import org.facens.grupo_6_gameeducator.service.agente.AgenteService.RespostaDoAgente;
import org.facens.grupo_6_gameeducator.service.agente.FerramentasDeAcompanhamento;
import org.facens.grupo_6_gameeducator.service.agente.FerramentasDeOrientacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * SCENARIOS RF04 (AF): agentes de IA coordenam orientacao e acompanhamento COM AUTORIZACAO DO USUARIO.
 *
 * <p>O LLM e dublado por um "roteiro" que chama as ferramentas como o modelo faria: o que se testa e o que as
 * ferramentas podem ver e fazer (RNF03) e que nenhuma acao roda sem aprovacao.
 */
@DisplayName("RF04 - agentes de IA com autorizacao do usuario")
class AgentesDeIaComAutorizacaoTest extends CenarioBase {

    private static final int CORRETA = 1;

    @MockitoBean
    private OrquestradorDeAgente orquestrador;
    @MockitoBean
    private ServicoDeIa servicoDeIa;
    @Autowired
    private AgenteService agenteService;
    @Autowired
    private AcoesDoAgenteService acoesService;
    @Autowired
    private MetaDeEstudoRepository metaRepository;
    @Autowired
    private OutboxEventoRepository outbox;

    private Usuario professor;
    private Usuario aluno;
    private Usuario colega;
    private Curso curso;
    private Missao missao;
    private Missao missaoDeOutroCurso;
    private PedidoAoAgente ultimoPedido;

    @BeforeEach
    void cenario() {
        professor = umProfessor("Ana");
        curso = umCurso("Matematica", professor);
        missao = missaoService.criar(professor.getId(), curso.getId(), missaoDeExemplo(60));
        Curso outro = umCurso("Fisica", professor);
        missaoDeOutroCurso = missaoService.criar(professor.getId(), outro.getId(), missaoDeExemplo(10));
        aluno = umAluno("Bruno");
        colega = umAluno("Carla");
        matricular(aluno, curso);
        matricular(colega, curso);
        jogoService.responder(aluno.getId(), missao.getDesafios().get(0).getId(), CORRETA);
    }

    /** O "LLM" de teste: recebe o pedido, executa o roteiro com as ferramentas e devolve um texto. */
    private void roteiro(Function<Object, String> passos) {
        when(orquestrador.conduzir(any())).thenAnswer(inv -> {
            ultimoPedido = inv.getArgument(0);
            return passos.apply(ultimoPedido.ferramentas().get(0));
        });
    }

    // ---------------- agente de orientacao (aluno) ----------------

    @Test
    @DisplayName("aluno conversa com o agente de ORIENTACAO, que le so os dados dele")
    void alunoUsaOrientacao() {
        when(servicoDeIa.recomendar(any())).thenReturn(new RecomendacoesDaIa("v1",
                List.of(new RecomendacoesDaIa.Item(missao.getId(), "Fracoes", 0.9, "continue"))));
        when(servicoDeIa.perguntar(any())).thenReturn(new RespostaDoAssistente("Some os numeradores", List.of(), "extrativo"));
        StringBuilder visto = new StringBuilder();
        roteiro(f -> {
            FerramentasDeOrientacao ferramentas = assertInstanceOf(FerramentasDeOrientacao.class, f);
            FerramentasDeOrientacao.Progresso progresso = ferramentas.consultarMeuProgresso();
            visto.append(progresso.xpTotal()).append('|').append(progresso.medalhas()).append('|')
                    .append(progresso.posicaoNoRanking()).append('|')
                    .append(ferramentas.recomendarMissoes().get(0).missaoId()).append('|')
                    .append(ferramentas.consultarMaterialDoCurso("como somar fracoes?"));
            return "Voce esta indo bem";
        });

        RespostaDoAgente resposta = agenteService.conversar(aluno.getId(), curso.getId(), "como estou?");

        assertEquals(AgenteService.ORIENTACAO, resposta.agente());
        assertEquals("Voce esta indo bem", resposta.resposta());
        assertEquals("60|[50]|1|" + missao.getId() + "|Some os numeradores", visto.toString());
        assertEquals("usuario-" + aluno.getId() + "-curso-" + curso.getId(), ultimoPedido.conversaId());
        assertTrue(ultimoPedido.instrucoes().contains("AGENTE DE ORIENTACAO"));
        assertTrue(ultimoPedido.instrucoes().contains("Matematica"));
        assertEquals("como estou?", ultimoPedido.mensagem());
    }

    @Test
    @DisplayName("aluno sem XP aparece sem posicao no ranking; material fora do ar vira aviso, nao erro")
    void orientacaoComDadosFaltando() {
        when(servicoDeIa.perguntar(any())).thenThrow(new IaIndisponivelException("fora", null));
        roteiro(f -> {
            FerramentasDeOrientacao ferramentas = (FerramentasDeOrientacao) f;
            return ferramentas.consultarMeuProgresso().posicaoNoRanking() + " " + ferramentas.consultarMaterialDoCurso("?");
        });

        String resposta = agenteService.conversar(colega.getId(), curso.getId(), "oi").resposta();

        assertEquals("null O material do curso esta indisponivel no momento.", resposta);
    }

    @Test
    @DisplayName("a meta de estudo proposta pelo agente fica PENDENTE e so vira meta quando o aluno aprova")
    void metaSoComAprovacao() {
        roteiro(f -> ((FerramentasDeOrientacao) f).proporMetaDeEstudo(missao.getId(), 7, "terminar fracoes"));

        RespostaDoAgente resposta = agenteService.conversar(aluno.getId(), curso.getId(), "crie uma meta");

        assertTrue(resposta.resposta().contains("AGUARDANDO APROVACAO"));
        AcaoDoAgente proposta = resposta.acoesPendentes().get(0);
        assertEquals(TipoAcaoDoAgente.META_DE_ESTUDO, proposta.getTipo());
        assertEquals(StatusAcaoDoAgente.PENDENTE, proposta.getStatus());
        assertTrue(metaRepository.findAll().isEmpty(), "nada e criado sem aprovacao");

        AcaoDoAgente aprovada = acoesService.aprovar(aluno.getId(), curso.getId(), proposta.getId());

        assertEquals(StatusAcaoDoAgente.EXECUTADA, aprovada.getStatus());
        assertTrue(aprovada.getDecididaEm() != null);
        var metas = acoesService.metas(aluno.getId(), curso.getId());
        assertEquals(1, metas.size());
        assertEquals(missao.getId(), metas.get(0).getMissaoId());
        assertEquals(proposta.getId(), metas.get(0).getAcaoId());
        assertTrue(metas.get(0).getDescricao().contains("terminar fracoes"));
        assertTrue(acoesService.pendentes(aluno.getId(), curso.getId()).isEmpty());
        assertEquals(1, acoesService.listar(aluno.getId(), curso.getId()).size());
    }

    @Test
    @DisplayName("o agente nao consegue propor meta para missao de outro curso nem com prazo fora de 1..90 dias")
    void metaInvalidaVoltaComoTexto() {
        roteiro(f -> {
            FerramentasDeOrientacao ferramentas = (FerramentasDeOrientacao) f;
            return ferramentas.proporMetaDeEstudo(missaoDeOutroCurso.getId(), 7, "x") + " / "
                    + ferramentas.proporMetaDeEstudo(missao.getId(), 0, null) + " / "
                    + ferramentas.proporMetaDeEstudo(missao.getId(), 91, null);
        });

        RespostaDoAgente resposta = agenteService.conversar(aluno.getId(), curso.getId(), "meta");

        assertTrue(resposta.resposta().startsWith("Nao foi possivel propor a meta: A missao"));
        assertTrue(resposta.resposta().contains("O prazo deve ficar entre 1 e 90 dias"));
        assertTrue(resposta.acoesPendentes().isEmpty());
    }

    @Test
    @DisplayName("rejeitar descarta a proposta; uma acao decidida nao pode ser decidida de novo")
    void rejeitarEDecidirUmaVez() {
        AcaoDoAgente proposta = acoesService.proporMetaDeEstudo(aluno.getId(), curso.getId(), missao.getId(), 5, "x");

        AcaoDoAgente rejeitada = acoesService.rejeitar(aluno.getId(), curso.getId(), proposta.getId());

        assertEquals(StatusAcaoDoAgente.REJEITADA, rejeitada.getStatus());
        assertTrue(metaRepository.findAll().isEmpty());
        assertThrows(RegraDeNegocioException.class,
                () -> acoesService.aprovar(aluno.getId(), curso.getId(), proposta.getId()));
        assertThrows(RegraDeNegocioException.class,
                () -> acoesService.rejeitar(aluno.getId(), curso.getId(), proposta.getId()));
    }

    @Test
    @DisplayName("so quem conversou com o agente decide a acao; acao de outro curso nao existe")
    void soODonoDecide() {
        AcaoDoAgente proposta = acoesService.proporMetaDeEstudo(aluno.getId(), curso.getId(), missao.getId(), 5, "x");

        assertThrows(AcessoNegadoException.class,
                () -> acoesService.aprovar(professor.getId(), curso.getId(), proposta.getId()));
        assertThrows(AcessoNegadoException.class,
                () -> acoesService.aprovar(colega.getId(), curso.getId(), proposta.getId()));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> acoesService.aprovar(aluno.getId(), missaoDeOutroCurso.getCurso().getId(), proposta.getId()));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> acoesService.rejeitar(aluno.getId(), curso.getId(), -1L));
    }

    // ---------------- agente de acompanhamento (professor) ----------------

    @Test
    @DisplayName("professor conversa com o agente de ACOMPANHAMENTO, que ve a turma do curso dele")
    void professorUsaAcompanhamento() {
        StringBuilder visto = new StringBuilder();
        roteiro(f -> {
            FerramentasDeAcompanhamento ferramentas = assertInstanceOf(FerramentasDeAcompanhamento.class, f);
            ferramentas.consultarDesempenhoDaTurma().forEach(d -> visto.append(d.nome()).append(':').append(d.xpTotal())
                    .append(':').append(d.acertos()).append(' '));
            visto.append('#').append(ferramentas.consultarRankingDaTurma().get(0).nomeAluno());
            return "Carla precisa de ajuda";
        });

        RespostaDoAgente resposta = agenteService.conversar(professor.getId(), curso.getId(), "como esta a turma?");

        assertEquals(AgenteService.ACOMPANHAMENTO, resposta.agente());
        assertTrue(visto.toString().contains("Bruno:60:1"));
        assertTrue(visto.toString().contains("Carla:0:0"));
        assertTrue(visto.toString().endsWith("#Bruno"));
        assertTrue(ultimoPedido.instrucoes().contains("AGENTE DE ACOMPANHAMENTO"));
    }

    @Test
    @DisplayName("lembrete aprovado pelo professor vai para o outbox (RabbitMQ -> notificacao); antes disso, nada sai")
    void lembreteSoComAprovacao() {
        roteiro(f -> ((FerramentasDeAcompanhamento) f).proporLembreteAoAluno("carla", "Bora comecar a missao!"));

        RespostaDoAgente resposta = agenteService.conversar(professor.getId(), curso.getId(), "lembre a Carla");

        AcaoDoAgente proposta = resposta.acoesPendentes().get(0);
        assertEquals(TipoAcaoDoAgente.LEMBRETE_AO_ALUNO, proposta.getTipo());
        assertEquals(colega.getId(), proposta.getAlunoAlvoId());
        assertTrue(proposta.getDescricao().contains("Carla"));
        assertTrue(lembretes().isEmpty(), "nada e enviado sem aprovacao");

        acoesService.aprovar(professor.getId(), curso.getId(), proposta.getId());

        List<OutboxEvento> enviados = lembretes();
        assertEquals(1, enviados.size());
        assertTrue(enviados.get(0).getPayload().contains("\"alunoId\":" + colega.getId()));
        assertTrue(enviados.get(0).getPayload().contains("Bora comecar a missao!"));
    }

    @Test
    @DisplayName("o agente nao propoe lembrete para aluno fora da turma nem sem mensagem")
    void lembreteInvalidoVoltaComoTexto() {
        Usuario deFora = umAluno("Davi");
        roteiro(f -> {
            FerramentasDeAcompanhamento ferramentas = (FerramentasDeAcompanhamento) f;
            return ferramentas.proporLembreteAoAluno(String.valueOf(deFora.getId()), "oi") + " / "
                    + ferramentas.proporLembreteAoAluno("Davi", "oi") + " / "
                    + ferramentas.proporLembreteAoAluno(" ", "oi") + " / "
                    + ferramentas.proporLembreteAoAluno(null, "oi") + " / "
                    + ferramentas.proporLembreteAoAluno("r", "oi") + " / "
                    + ferramentas.proporLembreteAoAluno(String.valueOf(colega.getId()), " ") + " / "
                    + ferramentas.proporLembreteAoAluno("Cárla", null);
        });

        RespostaDoAgente resposta = agenteService.conversar(professor.getId(), curso.getId(), "lembrete");

        assertTrue(resposta.resposta().contains("nao esta matriculado neste curso"), "id de fora da turma");
        assertTrue(resposta.resposta().contains("Nenhum aluno da turma se chama \"Davi\""), "nome de fora da turma");
        assertTrue(resposta.resposta().contains("Informe o nome do aluno"));
        assertTrue(resposta.resposta().contains("Mais de um aluno corresponde a \"r\": [Bruno, Carla]"));
        assertTrue(resposta.resposta().contains("O lembrete precisa de uma mensagem"));
        assertTrue(resposta.acoesPendentes().isEmpty());
    }

    @Test
    @DisplayName("se o aluno saiu da turma depois da proposta, a aprovacao do lembrete e recusada")
    void lembreteParaAlunoQueSaiu() {
        AcaoDoAgente proposta = acoesService.proporLembrete(professor.getId(), curso.getId(), colega.getId(), "oi");
        matriculaRepository.findByCursoId(curso.getId()).stream()
                .filter(m -> m.getAluno().getId().equals(colega.getId()))
                .forEach(matriculaRepository::delete);

        assertThrows(RegraDeNegocioException.class,
                () -> acoesService.aprovar(professor.getId(), curso.getId(), proposta.getId()));
        assertTrue(lembretes().isEmpty());
    }

    // ---------------- quem pode conversar (RNF03) e degradacao ----------------

    @Test
    @DisplayName("so conversa quem participa do curso: aluno matriculado ou professor responsavel")
    void controleDeAcesso() {
        Usuario deFora = umAluno("Davi");
        Usuario outroProfessor = umProfessor("Beto");

        assertThrows(AcessoNegadoException.class, () -> agenteService.conversar(deFora.getId(), curso.getId(), "oi"));
        assertThrows(AcessoNegadoException.class,
                () -> agenteService.conversar(outroProfessor.getId(), curso.getId(), "oi"));
        assertThrows(RecursoNaoEncontradoException.class, () -> agenteService.conversar(-1L, curso.getId(), "oi"));
        assertThrows(RecursoNaoEncontradoException.class, () -> agenteService.conversar(aluno.getId(), -1L, "oi"));
    }

    @Test
    @DisplayName("LLM fora do ar -> 503 do agente; as demais funcoes do curso seguem")
    void llmIndisponivel() {
        when(orquestrador.conduzir(any())).thenThrow(new IaIndisponivelException("ollama fora", null));

        ServicoIndisponivelException erro = assertThrows(ServicoIndisponivelException.class,
                () -> agenteService.conversar(aluno.getId(), curso.getId(), "oi"));

        assertTrue(erro.getMessage().contains("indisponivel"));
        assertEquals(60, jogoService.xpNoCurso(aluno.getId(), curso.getId()));
    }

    private List<OutboxEvento> lembretes() {
        return outbox.findAll().stream().filter(e -> e.getTipo().equals(LembreteAoAlunoEnviado.TIPO)).toList();
    }
}
