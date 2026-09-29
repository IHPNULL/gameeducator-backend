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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Post do forum de um curso. */
@Entity
@Table(name = "post")
@Getter
@Setter
@NoArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, length = 4000)
    private String conteudo;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<RespostaPost> respostas = new ArrayList<>();

    public Post(Curso curso, Usuario autor, String titulo, String conteudo) {
        this.curso = curso;
        this.autor = autor;
        this.titulo = titulo;
        this.conteudo = conteudo;
        this.dataHora = LocalDateTime.now();
    }

    public void adicionarResposta(RespostaPost resposta) {
        resposta.setPost(this);
        respostas.add(resposta);
    }
}
