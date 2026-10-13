package org.facens.grupo_6_gameeducator.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.facens.grupo_6_gameeducator.ia.adaptador.ClienteHttpIa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Adaptador HTTP contra um servidor HTTP real (JDK): contrato JSON, timeout de leitura,
 * erro 5xx e conexao recusada viram {@link IaIndisponivelException}.
 */
@DisplayName("Cliente HTTP do servico de IA")
class ClienteHttpIaTest {

    private HttpServer servidor;
    private volatile String corpo = "{}";
    private volatile int status = 200;
    private volatile long atrasoMs;

    @BeforeEach
    void subir() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", troca -> {
            try {
                Thread.sleep(atrasoMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(status, bytes.length);
            try (OutputStream saida = troca.getResponseBody()) {
                saida.write(bytes);
            }
        });
        servidor.start();
    }

    @AfterEach
    void derrubar() {
        servidor.stop(0);
    }

    private ClienteHttpIa cliente(int porta) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(300));
        fabrica.setReadTimeout(Duration.ofMillis(500));
        return new ClienteHttpIa(RestClient.builder().baseUrl("http://127.0.0.1:" + porta).requestFactory(fabrica).build());
    }

    private ClienteHttpIa cliente() {
        return cliente(servidor.getAddress().getPort());
    }

    @Test
    @DisplayName("recomendacao: le o JSON camelCase do FastAPI")
    void recomendacao() {
        corpo = """
                {"modeloVersao":"baseline-0.1","itens":[{"missaoId":7,"titulo":"Fracoes","pontuacao":0.8,"motivo":"m","extra":1}]}
                """;

        RecomendacoesDaIa resposta = cliente().recomendar(new PedidoDeRecomendacao(1L, 2L, List.of(), 3));

        assertEquals("baseline-0.1", resposta.modeloVersao());
        assertEquals(7L, resposta.itens().get(0).missaoId());
    }

    @Test
    @DisplayName("assistente: le resposta e fontes")
    void assistente() {
        corpo = """
                {"resposta":"Com base no material","fontes":[{"id":"missao-1","titulo":"Fracoes","relevancia":0.4}],"provedor":"extrativo"}
                """;

        RespostaDoAssistente resposta = cliente().perguntar(new PerguntaAoAssistente(2L, "o que e?", List.of()));

        assertEquals("missao-1", resposta.fontes().get(0).id());
        assertEquals("extrativo", resposta.provedor());
    }

    @Test
    @DisplayName("IA lenta: estoura o timeout de leitura em vez de prender a thread do Core")
    void timeout() {
        atrasoMs = 1500;
        long inicio = System.nanoTime();

        assertThrows(IaIndisponivelException.class, () -> cliente().recomendar(new PedidoDeRecomendacao(1L, 2L, List.of(), 3)));

        long decorrido = Duration.ofNanos(System.nanoTime() - inicio).toMillis();
        assertTrue(decorrido < 1400, "deveria desistir perto de 500ms, levou " + decorrido + "ms");
    }

    @Test
    @DisplayName("IA com erro 5xx vira IaIndisponivelException")
    void erro5xx() {
        status = 500;

        assertThrows(IaIndisponivelException.class, () -> cliente().perguntar(new PerguntaAoAssistente(2L, "?", List.of())));
    }

    @Test
    @DisplayName("IA fora do ar (conexao recusada) vira IaIndisponivelException")
    void conexaoRecusada() throws IOException {
        int portaLivre;
        try (ServerSocket socket = new ServerSocket(0)) {
            portaLivre = socket.getLocalPort();
        }

        assertThrows(IaIndisponivelException.class, () -> cliente(portaLivre).recomendar(new PedidoDeRecomendacao(1L, 2L, List.of(), 3)));
    }

    @Test
    @DisplayName("resposta sem corpo vira IaIndisponivelException")
    void semCorpo() {
        status = 204;
        corpo = "";

        assertThrows(IaIndisponivelException.class, () -> cliente().recomendar(new PedidoDeRecomendacao(1L, 2L, List.of(), 3)));
    }
}
