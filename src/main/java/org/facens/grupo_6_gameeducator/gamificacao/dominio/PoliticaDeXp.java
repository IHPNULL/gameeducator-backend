package org.facens.grupo_6_gameeducator.gamificacao.dominio;

/** Regra de credito de XP: o XP de um desafio so e creditado no primeiro acerto (anti-farm). */
public final class PoliticaDeXp {

    private PoliticaDeXp() {
    }

    public static int xpACreditar(boolean correta, boolean jaHaviaAcertado, int xpDoDesafio) {
        return (correta && !jaHaviaAcertado) ? xpDoDesafio : 0;
    }
}
