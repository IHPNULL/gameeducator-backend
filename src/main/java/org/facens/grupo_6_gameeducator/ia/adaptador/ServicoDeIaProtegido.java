package org.facens.grupo_6_gameeducator.ia.adaptador;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;
import org.facens.grupo_6_gameeducator.ia.IaIndisponivelException;
import org.facens.grupo_6_gameeducator.ia.PedidoDeRecomendacao;
import org.facens.grupo_6_gameeducator.ia.PerguntaAoAssistente;
import org.facens.grupo_6_gameeducator.ia.RecomendacoesDaIa;
import org.facens.grupo_6_gameeducator.ia.RespostaDoAssistente;
import org.facens.grupo_6_gameeducator.ia.ServicoDeIa;

/**
 * Decorador da porta de IA: disjuntor + metricas. As metricas sao a evidencia do Desafio 6 de que
 * a decisao do Desafio 3 funciona em execucao ({@code gameeducator_ia_chamadas_total{resultado}},
 * {@code gameeducator_ia_latencia_seconds}, {@code gameeducator_ia_disjuntor_aberto}).
 */
public class ServicoDeIaProtegido implements ServicoDeIa {

    private final ServicoDeIa alvo;
    private final Disjuntor disjuntor;
    private final MeterRegistry registry;

    public ServicoDeIaProtegido(ServicoDeIa alvo, Disjuntor disjuntor, MeterRegistry registry) {
        this.alvo = alvo;
        this.disjuntor = disjuntor;
        this.registry = registry;
        Gauge.builder("gameeducator.ia.disjuntor.aberto", disjuntor, d -> d.estado() == Disjuntor.Estado.ABERTO ? 1 : 0)
                .description("1 quando o disjuntor da IA esta aberto (Core em modo degradado)")
                .register(registry);
    }

    @Override
    public RecomendacoesDaIa recomendar(PedidoDeRecomendacao pedido) {
        return executar("recomendacao", () -> alvo.recomendar(pedido));
    }

    @Override
    public RespostaDoAssistente perguntar(PerguntaAoAssistente pergunta) {
        return executar("assistente", () -> alvo.perguntar(pergunta));
    }

    private <T> T executar(String operacao, Supplier<T> chamada) {
        if (!disjuntor.permiteChamada()) {
            contar(operacao, "disjuntor_aberto");
            throw new IaIndisponivelException("Disjuntor da IA aberto: chamada nao realizada", null);
        }
        Timer.Sample amostra = Timer.start(registry);
        try {
            T resultado = chamada.get();
            disjuntor.registrarSucesso();
            contar(operacao, "sucesso");
            return resultado;
        } catch (IaIndisponivelException e) {
            disjuntor.registrarFalha();
            contar(operacao, "falha");
            throw e;
        } finally {
            amostra.stop(Timer.builder("gameeducator.ia.latencia")
                    .description("Latencia das chamadas do Core ao servico de IA")
                    .tag("operacao", operacao)
                    .publishPercentileHistogram()
                    .register(registry));
        }
    }

    private void contar(String operacao, String resultado) {
        Counter.builder("gameeducator.ia.chamadas")
                .description("Chamadas do Core ao servico de IA por resultado")
                .tag("operacao", operacao)
                .tag("resultado", resultado)
                .register(registry)
                .increment();
    }
}
