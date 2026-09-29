package org.facens.grupo_6_gameeducator.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "missao")
@Getter
@Setter
@NoArgsConstructor
public class Missao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(length = 1000)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @OneToMany(mappedBy = "missao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Desafio> desafios = new ArrayList<>();

    public Missao(String titulo, String descricao, Curso curso) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.curso = curso;
    }

    public void adicionarDesafio(Desafio desafio) {
        desafio.setMissao(this);
        desafios.add(desafio);
    }

    /** Soma do XP de todos os desafios: o maximo que o aluno pode ganhar nesta missao. */
    public int getXpTotal() {
        return desafios.stream().mapToInt(Desafio::getXp).sum();
    }
}
