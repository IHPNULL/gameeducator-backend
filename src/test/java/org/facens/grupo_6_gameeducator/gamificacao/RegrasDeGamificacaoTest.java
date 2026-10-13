package org.facens.grupo_6_gameeducator.gamificacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.facens.grupo_6_gameeducator.gamificacao.dominio.PoliticaDeMedalhas;
import org.facens.grupo_6_gameeducator.gamificacao.dominio.PoliticaDeXp;
import org.facens.grupo_6_gameeducator.gamificacao.dominio.Progresso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Desafio 2 - regras de dominio da gamificacao isoladas: JUnit puro, sem Spring, sem banco,
 * sem mocks. Se estes testes precisassem de contexto Spring, a inversao de dependencia falhou.
 */
@DisplayName("Regras de gamificacao (dominio puro)")
class RegrasDeGamificacaoTest {

    @Test
    @DisplayName("XP so e creditado no primeiro acerto do desafio")
    void xpSoNoPrimeiroAcerto() {
        assertEquals(10, PoliticaDeXp.xpACreditar(true, false, 10));
        assertEquals(0, PoliticaDeXp.xpACreditar(true, true, 10), "reacerto nao acumula XP");
        assertEquals(0, PoliticaDeXp.xpACreditar(false, false, 10), "erro nao credita XP");
    }

    @Test
    @DisplayName("progresso e imutavel e recusa XP negativo")
    void progressoImutavel() {
        Progresso inicial = Progresso.inicial(2L, 10L);
        Progresso depois = inicial.creditar(30);

        assertEquals(0, inicial.xpTotal());
        assertEquals(30, depois.xpTotal());
        assertThrows(IllegalArgumentException.class, () -> depois.creditar(-1));
        assertThrows(IllegalArgumentException.class, () -> new Progresso(2L, 10L, -5));
    }

    @Test
    @DisplayName("medalha para cada marco cruzado, sem repetir marco ja conquistado")
    void marcosCruzados() {
        PoliticaDeMedalhas politica = PoliticaDeMedalhas.padrao();

        assertEquals(List.of(50), politica.novasMedalhas(40, 50, Set.of()), "marco atingido exatamente conta");
        assertEquals(List.of(50, 100), politica.novasMedalhas(0, 120, Set.of()), "dois marcos de uma vez");
        assertEquals(List.of(100), politica.novasMedalhas(0, 120, Set.of(50)), "marco ja conquistado nao repete");
        assertEquals(List.of(), politica.novasMedalhas(50, 60, Set.of()), "marco ja ultrapassado antes nao conta");
    }

    @Test
    @DisplayName("marcos sao configuraveis: a regra muda sem mexer em codigo nem banco")
    void marcosConfiguraveis() {
        PoliticaDeMedalhas politica = new PoliticaDeMedalhas(List.of(30, 10, 30, 20));

        assertEquals(List.of(10, 20, 30), politica.marcos(), "ordenados e sem duplicidade");
        assertEquals(List.of(10, 20), politica.novasMedalhas(0, 25, Set.of()));
    }

    @Test
    @DisplayName("politica de medalhas invalida e recusada na criacao")
    void politicaInvalida() {
        assertThrows(IllegalArgumentException.class, () -> new PoliticaDeMedalhas(null));
        assertThrows(IllegalArgumentException.class, () -> new PoliticaDeMedalhas(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new PoliticaDeMedalhas(List.of(50, 0)));
        assertThrows(IllegalArgumentException.class, () -> new PoliticaDeMedalhas(Arrays.asList(50, null)));
    }
}
