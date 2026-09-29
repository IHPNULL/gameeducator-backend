package org.facens.grupo_6_gameeducator.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** XP acumulado por um aluno dentro de um curso: base do progresso e do ranking da turma. */
@Entity
@Table(
        name = "progresso_aluno",
        uniqueConstraints = @UniqueConstraint(name = "uk_progresso_aluno_curso", columnNames = {"aluno_id", "curso_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class ProgressoAluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Usuario aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @Column(name = "xp_total", nullable = false)
    private int xpTotal;

    public ProgressoAluno(Usuario aluno, Curso curso) {
        this.aluno = aluno;
        this.curso = curso;
        this.xpTotal = 0;
    }

    public void creditarXp(int xp) {
        if (xp < 0) {
            throw new IllegalArgumentException("XP creditado nao pode ser negativo");
        }
        this.xpTotal += xp;
    }
}
