package org.facens.grupo_6_gameeducator.domain;

/** Ciclo de vida de uma {@link AcaoDoAgente}: PENDENTE -> EXECUTADA (aprovada) | REJEITADA. */
public enum StatusAcaoDoAgente {
    PENDENTE,
    EXECUTADA,
    REJEITADA
}
