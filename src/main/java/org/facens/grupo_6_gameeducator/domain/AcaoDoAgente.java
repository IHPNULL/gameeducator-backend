package org.facens.grupo_6_gameeducator.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * RF04 - acao que um agente de IA PROPOS e que so roda com autorizacao do usuario.
 *
 * <p>O agente nunca altera dados por conta propria: a ferramenta de acao grava esta proposta como
 * {@link StatusAcaoDoAgente#PENDENTE}; o usuario aprova (e o Core executa) ou rejeita. Guarda ids, e nao
 * associacoes JPA, para ser lida fora de transacao e servir de trilha de auditoria.
 */
@Entity
@Table(name = "acao_do_agente", indexes = @Index(name = "idx_acao_solicitante_curso", columnList = "solicitante_id, curso_id"))
@Getter
@NoArgsConstructor
public class AcaoDoAgente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario dono da conversa com o agente: o unico que pode aprovar ou rejeitar. */
    @Column(name = "solicitante_id", nullable = false)
    private Long solicitanteId;

    @Column(name = "curso_id", nullable = false)
    private Long cursoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAcaoDoAgente tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAcaoDoAgente status;

    /** Texto mostrado ao usuario na hora de decidir. */
    @Column(nullable = false, length = 500)
    private String descricao;

    /** META_DE_ESTUDO: missao alvo. */
    private Long missaoId;

    /** META_DE_ESTUDO: data limite. */
    private LocalDate prazo;

    /** LEMBRETE_AO_ALUNO: destinatario. */
    private Long alunoAlvoId;

    /** LEMBRETE_AO_ALUNO: texto do lembrete. */
    @Column(length = 500)
    private String mensagem;

    @Column(nullable = false)
    private Instant criadaEm;

    private Instant decididaEm;

    public static AcaoDoAgente metaDeEstudo(Long alunoId, Long cursoId, Long missaoId, String tituloMissao,
                                            LocalDate prazo, String objetivo, Instant agora) {
        AcaoDoAgente acao = nova(alunoId, cursoId, TipoAcaoDoAgente.META_DE_ESTUDO, agora);
        acao.missaoId = missaoId;
        acao.prazo = prazo;
        acao.descricao = limitar("Meta: concluir \"" + tituloMissao + "\" ate " + prazo + ". " + objetivo, 500);
        return acao;
    }

    public static AcaoDoAgente lembreteAoAluno(Long professorId, Long cursoId, Long alunoId, String nomeAluno,
                                               String mensagem, Instant agora) {
        AcaoDoAgente acao = nova(professorId, cursoId, TipoAcaoDoAgente.LEMBRETE_AO_ALUNO, agora);
        acao.alunoAlvoId = alunoId;
        acao.mensagem = limitar(mensagem, 500);
        acao.descricao = limitar("Enviar lembrete para " + nomeAluno + ": \"" + mensagem + "\"", 500);
        return acao;
    }

    private static AcaoDoAgente nova(Long solicitanteId, Long cursoId, TipoAcaoDoAgente tipo, Instant agora) {
        AcaoDoAgente acao = new AcaoDoAgente();
        acao.solicitanteId = solicitanteId;
        acao.cursoId = cursoId;
        acao.tipo = tipo;
        acao.status = StatusAcaoDoAgente.PENDENTE;
        acao.criadaEm = agora;
        return acao;
    }

    public boolean pendente() {
        return status == StatusAcaoDoAgente.PENDENTE;
    }

    public void marcarExecutada(Instant agora) {
        status = StatusAcaoDoAgente.EXECUTADA;
        decididaEm = agora;
    }

    public void marcarRejeitada(Instant agora) {
        status = StatusAcaoDoAgente.REJEITADA;
        decididaEm = agora;
    }

    private static String limitar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
