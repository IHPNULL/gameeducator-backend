package org.facens.grupo_6_gameeducator.eventos.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Linha do transactional outbox: o evento fica pendente ate o relay confirmar a publicacao no broker. */
@Entity
@Table(name = "outbox_evento", indexes = @Index(name = "ix_outbox_pendentes", columnList = "publicado_em, criado_em"))
@Getter
@NoArgsConstructor
public class OutboxEvento {

    @Id
    @Column(name = "evento_id", length = 36)
    private String eventoId;

    @Column(nullable = false, length = 100)
    private String tipo;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "publicado_em")
    private Instant publicadoEm;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "ultimo_erro", length = 500)
    private String ultimoErro;

    public OutboxEvento(String eventoId, String tipo, String payload, Instant criadoEm) {
        this.eventoId = eventoId;
        this.tipo = tipo;
        this.payload = payload;
        this.criadoEm = criadoEm;
    }

    public void marcarPublicado(Instant quando) {
        this.publicadoEm = quando;
        this.tentativas++;
        this.ultimoErro = null;
    }

    public void registrarFalha(String erro) {
        this.tentativas++;
        this.ultimoErro = erro == null ? null : erro.substring(0, Math.min(erro.length(), 500));
    }

    public boolean publicado() {
        return publicadoEm != null;
    }
}
