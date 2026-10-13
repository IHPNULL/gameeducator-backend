package org.facens.grupo_6_gameeducator.gamificacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import org.facens.grupo_6_gameeducator.gamificacao.adaptador.memoria.GamificacaoEmMemoria;
import org.facens.grupo_6_gameeducator.gamificacao.porta.RepositorioDeProgresso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Desafio 2 - a aplicacao inteira sobe com o adaptador em memoria trocando apenas uma propriedade.
 * Nenhuma classe de dominio, aplicacao, service ou controller muda.
 */
@SpringBootTest(properties = {"app.gamificacao.persistencia=memoria", "app.gamificacao.marcos-xp=10,20"})
@DisplayName("Troca de adaptador por configuracao (persistencia=memoria)")
class TrocaDeAdaptadorNoContextoTest {

    @Autowired
    private RepositorioDeProgresso progressos;
    @Autowired
    private GamificacaoApi gamificacao;

    @Test
    @DisplayName("com persistencia=memoria a porta e atendida pelo adaptador em memoria e os marcos vem da config")
    void adaptadorEmMemoriaNoContexto() {
        assertInstanceOf(GamificacaoEmMemoria.class, progressos);

        ResultadoPontuacao resultado = gamificacao.pontuarResposta(new RespostaPontuavel(7L, 70L, 25, true, false));

        assertEquals(List.of(10, 20), resultado.medalhasConquistadas());
    }
}
