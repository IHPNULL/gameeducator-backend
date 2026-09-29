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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Registro de cada resposta enviada pelo aluno: alimenta o historico da US "ver minhas tentativas". */
@Entity
@Table(name = "tentativa")
@Getter
@Setter
@NoArgsConstructor
public class Tentativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Usuario aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "desafio_id", nullable = false)
    private Desafio desafio;

    @Column(name = "indice_resposta", nullable = false)
    private int indiceResposta;

    @Column(nullable = false)
    private boolean correta;

    @Column(name = "xp_ganho", nullable = false)
    private int xpGanho;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    public Tentativa(Usuario aluno, Desafio desafio, int indiceResposta, boolean correta, int xpGanho) {
        this.aluno = aluno;
        this.desafio = desafio;
        this.indiceResposta = indiceResposta;
        this.correta = correta;
        this.xpGanho = xpGanho;
        this.dataHora = LocalDateTime.now();
    }
}
