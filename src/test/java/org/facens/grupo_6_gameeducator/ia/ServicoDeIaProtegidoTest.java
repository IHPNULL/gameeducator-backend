package org.facens.grupo_6_gameeducator.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.facens.grupo_6_gameeducator.ia.adaptador.Disjuntor;
import org.facens.grupo_6_gameeducator.ia.adaptador.ServicoDeIaProtegido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Protecao da porta de IA (disjuntor + metricas)")
class ServicoDeIaProtegidoTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final Disjuntor disjuntor = new Disjuntor(2, Duration.ofMinutes(1), Clock.systemUTC());
    private boolean iaNoAr = true;
    private int chamadasReais;

    private final RecomendacoesDaIa recomendacoes = new RecomendacoesDaIa("baseline-0.1", List.of());
    private final RespostaDoAssistente resposta = new RespostaDoAssistente("ok", List.of(), "extrativo");

    private final ServicoDeIa falso = new ServicoDeIa() {
        @Override
        public RecomendacoesDaIa recomendar(PedidoDeRecomendacao pedido) {
            return responder(recomendacoes);
        }

        @Override
        public RespostaDoAssistente perguntar(PerguntaAoAssistente pergunta) {
            return responder(resposta);
        }

        private <T> T responder(T valor) {
            chamadasReais++;
            if (!iaNoAr) {
                throw new IaIndisponivelException("IA fora", null);
            }
            return valor;
        }
    };

    private final ServicoDeIa protegido = new ServicoDeIaProtegido(falso, disjuntor, registry);

    private double contador(String operacao, String resultado) {
        return registry.counter("gameeducator.ia.chamadas", "operacao", operacao, "resultado", resultado).count();
    }

    @Test
    @DisplayName("sucesso passa adiante e e contado")
    void sucesso() {
        assertSame(recomendacoes, protegido.recomendar(null));
        assertSame(resposta, protegido.perguntar(null));

        assertEquals(1, contador("recomendacao", "sucesso"));
        assertEquals(1, contador("assistente", "sucesso"));
        assertEquals(2, registry.find("gameeducator.ia.latencia").timers().stream().mapToLong(t -> t.count()).sum());
    }

    @Test
    @DisplayName("com a IA fora, depois do limite o disjuntor abre e a IA nem e chamada (falha rapida)")
    void disjuntorAbreEPouparIa() {
        iaNoAr = false;
        assertThrows(IaIndisponivelException.class, () -> protegido.recomendar(null));
        assertThrows(IaIndisponivelException.class, () -> protegido.recomendar(null));

        assertThrows(IaIndisponivelException.class, () -> protegido.recomendar(null));

        assertEquals(2, chamadasReais, "a terceira chamada foi barrada pelo disjuntor");
        assertEquals(2, contador("recomendacao", "falha"));
        assertEquals(1, contador("recomendacao", "disjuntor_aberto"));
        assertEquals(1.0, registry.get("gameeducator.ia.disjuntor.aberto").gauge().value());
    }
}
