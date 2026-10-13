package org.facens.grupo_6_gameeducator.gamificacao.dominio;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Regra de medalhas: cada marco de XP cruzado concede uma medalha, uma unica vez por marco.
 *
 * <p>Os marcos sao parametro da politica (e nao constantes no service, como na AC1), porque as
 * regras de gamificacao mudam com frequencia: trocar os marcos nao exige mexer em codigo nem banco
 * (propriedade {@code app.gamificacao.marcos-xp}).
 */
public final class PoliticaDeMedalhas {

    public static final List<Integer> MARCOS_PADRAO = List.of(50, 100, 250, 500);

    private final List<Integer> marcos;

    public PoliticaDeMedalhas(List<Integer> marcos) {
        if (marcos == null || marcos.isEmpty()) {
            throw new IllegalArgumentException("A politica de medalhas precisa de ao menos um marco de XP");
        }
        if (marcos.stream().anyMatch(marco -> marco == null || marco <= 0)) {
            throw new IllegalArgumentException("Marcos de XP precisam ser positivos");
        }
        this.marcos = marcos.stream().distinct().sorted().toList();
    }

    public static PoliticaDeMedalhas padrao() {
        return new PoliticaDeMedalhas(MARCOS_PADRAO);
    }

    public List<Integer> marcos() {
        return marcos;
    }

    /**
     * Marcos cruzados entre {@code xpAntes} (exclusive) e {@code xpDepois} (inclusive) que o aluno
     * ainda nao tem. Uma resposta que cruza varios marcos concede todos eles.
     */
    public List<Integer> novasMedalhas(int xpAntes, int xpDepois, Set<Integer> jaConquistadas) {
        Objects.requireNonNull(jaConquistadas, "jaConquistadas");
        return marcos.stream()
                .filter(marco -> xpAntes < marco && xpDepois >= marco)
                .filter(marco -> !jaConquistadas.contains(marco))
                .toList();
    }
}
