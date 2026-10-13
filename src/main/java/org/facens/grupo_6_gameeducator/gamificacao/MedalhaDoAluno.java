package org.facens.grupo_6_gameeducator.gamificacao;

import java.time.LocalDateTime;

/** Medalha conquistada pelo aluno em um curso. */
public record MedalhaDoAluno(int marcoXp, LocalDateTime dataConcedida) {
}
