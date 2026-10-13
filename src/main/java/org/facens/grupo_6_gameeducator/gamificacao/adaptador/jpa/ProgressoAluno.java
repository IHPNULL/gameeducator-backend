package org.facens.grupo_6_gameeducator.gamificacao.adaptador.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Usuario;

/**
 * Mapeamento JPA do progresso de XP (tabela da AC1, inalterada). Interno ao adaptador: a regra
 * de credito de XP mora em {@code gamificacao.dominio.Progresso}.
 *
 * <p>O indice (curso_id, xp_total) sustenta o ranking da turma, que e a leitura que mais cresce
 * (ver evidencia do Desafio 1 em docs/arquitetura).
 */
@Entity
@Table(
        name = "progresso_aluno",
        uniqueConstraints = @UniqueConstraint(name = "uk_progresso_aluno_curso", columnNames = {"aluno_id", "curso_id"}),
        indexes = @Index(name = "ix_progresso_curso_xp", columnList = "curso_id, xp_total DESC")
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
}
