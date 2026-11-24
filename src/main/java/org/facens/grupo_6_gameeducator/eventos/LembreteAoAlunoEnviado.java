package org.facens.grupo_6_gameeducator.eventos;

import java.time.Instant;

/**
 * RF04 - o professor aprovou um lembrete proposto pelo agente de acompanhamento. Segue pelo outbox e pelo
 * RabbitMQ ate o servico de notificacao (assincrono: o aluno recebe depois, sem travar a aprovacao).
 */
public record LembreteAoAlunoEnviado(String eventoId, Long alunoId, Long cursoId, Long professorId, String mensagem,
                                     Long acaoId, Instant ocorridoEm) implements EventoDeDominio {

    public static final String TIPO = "agente.lembrete-ao-aluno";

    @Override
    public String tipo() {
        return TIPO;
    }
}
