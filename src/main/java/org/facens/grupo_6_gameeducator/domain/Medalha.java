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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Medalha concedida ao aluno ao atingir um marco de XP em um curso (ex.: 50, 100, 250, 500). */
@Entity
@Table(
        name = "medalha",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_medalha_aluno_curso_marco",
                columnNames = {"aluno_id", "curso_id", "marco_xp"})
)
@Getter
@Setter
@NoArgsConstructor
public class Medalha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Usuario aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @Column(name = "marco_xp", nullable = false)
    private int marcoXp;

    @Column(name = "data_concedida", nullable = false)
    private LocalDateTime dataConcedida;

    public Medalha(Usuario aluno, Curso curso, int marcoXp) {
        this.aluno = aluno;
        this.curso = curso;
        this.marcoXp = marcoXp;
        this.dataConcedida = LocalDateTime.now();
    }
}
