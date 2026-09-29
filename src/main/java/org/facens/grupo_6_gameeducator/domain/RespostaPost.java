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

/** Resposta de um usuario a um post do forum. */
@Entity
@Table(name = "resposta_post")
@Getter
@Setter
@NoArgsConstructor
public class RespostaPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(nullable = false, length = 4000)
    private String conteudo;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    public RespostaPost(Usuario autor, String conteudo) {
        this.autor = autor;
        this.conteudo = conteudo;
        this.dataHora = LocalDateTime.now();
    }
}
