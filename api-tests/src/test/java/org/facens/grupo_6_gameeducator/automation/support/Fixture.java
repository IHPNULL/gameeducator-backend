package org.facens.grupo_6_gameeducator.automation.support;

/**
 * IDs do cenario semeado por AutomationSeedData (backend, profile "automation").
 * Ver backend/src/main/java/.../config/AutomationSeedData.java para o cenario completo.
 */
public final class Fixture {

    public static final long PROFESSOR_ID = 1L;
    public static final long ALUNO_UM_ID = 2L;
    public static final long ALUNO_DOIS_ID = 3L;
    public static final long CURSO_ID = 1L;
    public static final long MISSAO_ID = 1L;
    public static final long DESAFIO_SOMA_ID = 1L;
    public static final long DESAFIO_CAPITAL_ID = 2L;
    public static final long POST_INICIAL_ID = 1L;

    /** ID que nunca existe no cenario semeado: usado para provocar 404. */
    public static final long ID_INEXISTENTE = 999999L;
    public static final long CURSO_INEXISTENTE = 999999L;

    private Fixture() {
    }
}
