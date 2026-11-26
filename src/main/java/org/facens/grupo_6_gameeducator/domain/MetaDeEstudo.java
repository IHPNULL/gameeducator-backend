package org.facens.grupo_6_gameeducator.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** RF04 - meta de estudo do aluno, criada quando ele aprova a proposta do agente de orientacao. */
@Entity
@Table(name = "meta_de_estudo")
@Getter
@NoArgsConstructor
public class MetaDeEstudo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long alunoId;

    @Column(nullable = false)
    private Long cursoId;

    @Column(nullable = false)
    private Long missaoId;

    @Column(nullable = false, length = 500)
    private String descricao;

    @Column(nullable = false)
    private LocalDate prazo;

    /** Proposta do agente que originou a meta (rastreabilidade da autorizacao). */
    @Column(nullable = false)
    private Long acaoId;

    @Column(nullable = false)
    private Instant criadaEm;

    public MetaDeEstudo(AcaoDoAgente acao, Instant agora) {
        this.alunoId = acao.getSolicitanteId();
        this.cursoId = acao.getCursoId();
        this.missaoId = acao.getMissaoId();
        this.descricao = acao.getDescricao();
        this.prazo = acao.getPrazo();
        this.acaoId = acao.getId();
        this.criadaEm = agora;
    }
}
