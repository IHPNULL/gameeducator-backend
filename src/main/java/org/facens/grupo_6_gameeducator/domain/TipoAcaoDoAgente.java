package org.facens.grupo_6_gameeducator.domain;

/** RF04 - acoes que os agentes podem propor (cada uma exige aprovacao do usuario). */
public enum TipoAcaoDoAgente {
    /** Agente de orientacao (aluno): criar uma meta de estudo para uma missao. */
    META_DE_ESTUDO,
    /** Agente de acompanhamento (professor): enviar um lembrete a um aluno da turma. */
    LEMBRETE_AO_ALUNO
}
