package org.facens.grupo_6_gameeducator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Missao;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.exception.AcessoNegadoException;
import org.facens.grupo_6_gameeducator.exception.ServicoIndisponivelException;
import org.facens.grupo_6_gameeducator.ia.IaIndisponivelException;
import org.facens.grupo_6_gameeducator.ia.PedidoDeRecomendacao;
import org.facens.grupo_6_gameeducator.ia.PerguntaAoAssistente;
import org.facens.grupo_6_gameeducator.ia.RecomendacoesDaIa;
import org.facens.grupo_6_gameeducator.ia.RespostaDoAssistente;
import org.facens.grupo_6_gameeducator.ia.ServicoDeIa;
import org.facens.grupo_6_gameeducator.service.AssistenteService;
import org.facens.grupo_6_gameeducator.service.RecomendacaoService;
import org.facens.grupo_6_gameeducator.service.dto.NovaMissaoRequest;
import org.facens.grupo_6_gameeducator.service.dto.NovoDesafioRequest;
import org.facens.grupo_6_gameeducator.service.dto.Recomendacoes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * SCENARIOS RF03/RF04 (Desafio 3): recomendacao e assistente, com a IA respondendo e fora do ar.
 * A porta {@link ServicoDeIa} e dublada: o que se testa aqui e o contexto enviado e o comportamento degradado.
 */
@DisplayName("Aluno usa recomendacao e assistente de IA")
class AlunoUsaAssistenteEIaTest extends CenarioBase {

    private static final int CORRETA = 1;

    @MockitoBean
    private ServicoDeIa servicoDeIa;
    @Autowired
    private RecomendacaoService recomendacaoService;
    @Autowired
    private AssistenteService assistenteService;

    private Curso curso;
    private Usuario aluno;
    private Missao fracoes;
    private Missao decimais;

    private void cenario() {
        Usuario professor = umProfessor("Ana");
        curso = umCurso("Matematica Basica", professor);
        fracoes = missaoService.criar(professor.getId(), curso.getId(), new NovaMissaoRequest("Fracoes", "Fracoes equivalentes",
                List.of(new NovoDesafioRequest("Quanto e 1/2 + 1/4 ?", List.of("1/6", "3/4"), CORRETA, 10))));
        decimais = missaoService.criar(professor.getId(), curso.getId(), new NovaMissaoRequest("Decimais", null,
                List.of(new NovoDesafioRequest("0,5 e igual a?", List.of("1/3", "1/2"), CORRETA, 10),
                        new NovoDesafioRequest("0,25 e igual a?", List.of("1/4", "1/5"), 0, 10))));
        aluno = umAluno("Bruno");
        matricular(aluno, curso);
        jogoService.responder(aluno.getId(), fracoes.getDesafios().get(0).getId(), CORRETA);
        jogoService.responder(aluno.getId(), decimais.getDesafios().get(0).getId(), 0);
    }

    @Test
    @DisplayName("com a IA no ar: o Core envia o progresso do aluno por missao e repassa as recomendacoes")
    void recomendacaoDaIa() {
        cenario();
        when(servicoDeIa.recomendar(any())).thenReturn(new RecomendacoesDaIa("baseline-0.1",
                List.of(new RecomendacoesDaIa.Item(decimais.getId(), "Decimais", 0.9, "continue"))));

        Recomendacoes recomendacoes = recomendacaoService.recomendar(aluno.getId(), curso.getId());

        assertEquals(Recomendacoes.ORIGEM_IA, recomendacoes.origem());
        assertEquals(decimais.getId(), recomendacoes.itens().get(0).missaoId());
        ArgumentCaptor<PedidoDeRecomendacao> pedido = ArgumentCaptor.forClass(PedidoDeRecomendacao.class);
        verify(servicoDeIa).recomendar(pedido.capture());
        PedidoDeRecomendacao.MissaoParaRecomendar resumoFracoes = pedido.getValue().missoes().get(0);
        PedidoDeRecomendacao.MissaoParaRecomendar resumoDecimais = pedido.getValue().missoes().get(1);
        assertTrue(resumoFracoes.concluida());
        assertEquals(1, resumoDecimais.tentativas());
        assertEquals(0, resumoDecimais.desafiosAcertados());
        assertEquals(2, resumoDecimais.totalDesafios());
    }

    @Test
    @DisplayName("com a IA fora do ar: recomendacao local (fallback) com as missoes nao concluidas, sem erro")
    void recomendacaoFallback() {
        cenario();
        when(servicoDeIa.recomendar(any())).thenThrow(new IaIndisponivelException("timeout", null));

        Recomendacoes recomendacoes = recomendacaoService.recomendar(aluno.getId(), curso.getId());

        assertEquals(Recomendacoes.ORIGEM_FALLBACK, recomendacoes.origem());
        assertEquals(List.of(decimais.getId()), recomendacoes.itens().stream().map(Recomendacoes.Item::missaoId).toList());
    }

    @Test
    @DisplayName("assistente: envia o material do curso (missoes + enunciados) e devolve a resposta da IA")
    void assistenteComIa() {
        cenario();
        when(servicoDeIa.perguntar(any())).thenReturn(new RespostaDoAssistente("resposta", List.of(), "extrativo"));

        RespostaDoAssistente resposta = assistenteService.perguntar(aluno.getId(), curso.getId(), "o que e fracao?");

        assertEquals("resposta", resposta.resposta());
        ArgumentCaptor<PerguntaAoAssistente> pedido = ArgumentCaptor.forClass(PerguntaAoAssistente.class);
        verify(servicoDeIa).perguntar(pedido.capture());
        assertEquals("missao-" + fracoes.getId(), pedido.getValue().documentos().get(0).id());
        assertTrue(pedido.getValue().documentos().get(0).texto().contains("Quanto e 1/2 + 1/4 ?"));
        assertTrue(pedido.getValue().documentos().get(1).texto().startsWith("0,5 e igual a?"), "missao sem descricao");
    }

    @Test
    @DisplayName("assistente com a IA fora do ar: 503 com mensagem clara (sem resposta inventada)")
    void assistenteIndisponivel() {
        cenario();
        when(servicoDeIa.perguntar(any())).thenThrow(new IaIndisponivelException("conexao recusada", null));

        ServicoIndisponivelException erro = assertThrows(ServicoIndisponivelException.class,
                () -> assistenteService.perguntar(aluno.getId(), curso.getId(), "o que e fracao?"));

        assertTrue(erro.getMessage().contains("indisponivel"));
    }

    @Test
    @DisplayName("aluno nao matriculado nao usa a IA do curso")
    void naoMatriculado() {
        cenario();
        Usuario visitante = umAluno("Carlos");

        assertThrows(AcessoNegadoException.class, () -> recomendacaoService.recomendar(visitante.getId(), curso.getId()));
        assertThrows(AcessoNegadoException.class, () -> assistenteService.perguntar(visitante.getId(), curso.getId(), "?"));
    }
}
