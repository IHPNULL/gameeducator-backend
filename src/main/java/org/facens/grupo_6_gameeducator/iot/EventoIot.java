package org.facens.grupo_6_gameeducator.iot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Evento de laboratorio aceito. A PK e o id gerado pelo dispositivo: e ela que garante a idempotencia. */
@Entity
@Table(name = "evento_iot", indexes = @Index(name = "ix_evento_iot_lab", columnList = "laboratorio, ocorrido_em"))
@Getter
@NoArgsConstructor
public class EventoIot {

    @Id
    @Column(name = "evento_id", length = 36)
    private String eventoId;

    @Column(nullable = false, length = 60)
    private String laboratorio;

    @Column(nullable = false, length = 60)
    private String dispositivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEventoIot tipo;

    @Column(name = "aluno_id", nullable = false)
    private Long alunoId;

    @Column(length = 120)
    private String atividade;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    @Column(name = "recebido_em", nullable = false)
    private Instant recebidoEm;

    public EventoIot(EventoDeLaboratorio evento, Instant recebidoEm) {
        this.eventoId = evento.eventoId();
        this.laboratorio = evento.laboratorio();
        this.dispositivo = evento.dispositivo();
        this.tipo = evento.tipo();
        this.alunoId = evento.alunoId();
        this.atividade = evento.atividade();
        this.ocorridoEm = evento.ocorridoEm();
        this.recebidoEm = recebidoEm;
    }
}
