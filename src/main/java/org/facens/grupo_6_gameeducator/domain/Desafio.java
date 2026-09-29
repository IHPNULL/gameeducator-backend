package org.facens.grupo_6_gameeducator.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "desafio")
@Getter
@Setter
@NoArgsConstructor
public class Desafio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String enunciado;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "desafio_alternativa", joinColumns = @JoinColumn(name = "desafio_id"))
    @OrderColumn(name = "ordem")
    @Column(name = "texto", nullable = false, length = 500)
    private List<String> alternativas = new ArrayList<>();

    /** Indice (base 0) da alternativa correta dentro de {@link #alternativas}. */
    @Column(name = "indice_resposta_correta", nullable = false)
    private int indiceRespostaCorreta;

    /** XP creditado ao aluno na primeira vez que ele acerta este desafio. */
    @Column(nullable = false)
    private int xp;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "missao_id", nullable = false)
    private Missao missao;

    public Desafio(String enunciado, List<String> alternativas, int indiceRespostaCorreta, int xp) {
        this.enunciado = enunciado;
        this.alternativas = new ArrayList<>(alternativas);
        this.indiceRespostaCorreta = indiceRespostaCorreta;
        this.xp = xp;
    }

    public boolean estaCorreta(int indiceResposta) {
        return indiceResposta == indiceRespostaCorreta;
    }
}
