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
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Nota de um aluno em uma avaliacao de um curso ("Prova 1", "Trabalho Final", ...). */
@Entity
@Table(
        name = "nota",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_nota_aluno_curso_avaliacao",
                columnNames = {"aluno_id", "curso_id", "avaliacao"})
)
@Getter
@Setter
@NoArgsConstructor
public class Nota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Usuario aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @Column(nullable = false)
    private String avaliacao;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal valor;

    public Nota(Usuario aluno, Curso curso, String avaliacao, BigDecimal valor) {
        this.aluno = aluno;
        this.curso = curso;
        this.avaliacao = avaliacao;
        setValor(valor);
    }

    /** Guarda sempre com 2 casas, para a nota exibida bater com a lancada. */
    public final void setValor(BigDecimal valor) {
        this.valor = valor == null ? null : valor.setScale(2, RoundingMode.HALF_UP);
    }
}
