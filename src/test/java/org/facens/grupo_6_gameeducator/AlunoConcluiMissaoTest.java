package org.facens.grupo_6_gameeducator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Missao;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.eventos.MedalhaConquistada;
import org.facens.grupo_6_gameeducator.eventos.MissaoConcluida;
import org.facens.grupo_6_gameeducator.eventos.outbox.OutboxEvento;
import org.facens.grupo_6_gameeducator.eventos.outbox.OutboxEventoRepository;
import org.facens.grupo_6_gameeducator.service.dto.NovaMissaoRequest;
import org.facens.grupo_6_gameeducator.service.dto.NovoDesafioRequest;
import org.facens.grupo_6_gameeducator.service.dto.ResultadoResposta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * SCENARIO (Desafio 4): ao concluir a missao, a resposta e imediata (sincrona) e os eventos para
 * notificacao/analytics ficam no outbox na MESMA transacao (sem depender do broker).
 */
@DisplayName("Aluno conclui missao e os eventos vao para o outbox")
class AlunoConcluiMissaoTest extends CenarioBase {

    private static final int CORRETA = 1;

    @Autowired
    private OutboxEventoRepository outbox;

    private Curso curso;
    private Usuario aluno;
    private Missao missao;

    private void cenario() {
        Usuario professor = umProfessor("Ana");
        curso = umCurso("Matematica Basica", professor);
        missao = missaoService.criar(professor.getId(), curso.getId(), new NovaMissaoRequest("Fracoes", "d",
                List.of(new NovoDesafioRequest("A", List.of("x", "y"), CORRETA, 30),
                        new NovoDesafioRequest("B", List.of("x", "y"), CORRETA, 30))));
        aluno = umAluno("Bruno");
        matricular(aluno, curso);
    }

    private List<OutboxEvento> eventosDoAluno(String tipo) {
        return outbox.findAll().stream()
                .filter(e -> e.getTipo().equals(tipo))
                .filter(e -> e.getPayload().contains("\"alunoId\":" + aluno.getId() + ","))
                .toList();
    }

    @Test
    @DisplayName("acertar o ultimo desafio conclui a missao e grava MissaoConcluida + MedalhaConquistada no outbox")
    void conclusaoGeraEventos() {
        cenario();

        ResultadoResposta primeiro = jogoService.responder(aluno.getId(), missao.getDesafios().get(0).getId(), CORRETA);
        ResultadoResposta ultimo = jogoService.responder(aluno.getId(), missao.getDesafios().get(1).getId(), CORRETA);

        assertFalse(primeiro.missaoConcluida());
        assertTrue(ultimo.missaoConcluida());
        List<OutboxEvento> concluidas = eventosDoAluno(MissaoConcluida.TIPO);
        assertEquals(1, concluidas.size());
        assertTrue(concluidas.get(0).getPayload().contains("\"missaoId\":" + missao.getId()));
        assertTrue(concluidas.get(0).getPayload().contains("\"xpTotalNoCurso\":60"));
        assertFalse(concluidas.get(0).publicado(), "fica pendente ate o relay publicar");
        assertEquals(1, eventosDoAluno(MedalhaConquistada.TIPO).size(), "cruzou o marco de 50 XP");
    }

    @Test
    @DisplayName("reacertar ou errar depois de concluida nao emite outra conclusao")
    void naoReemiteConclusao() {
        cenario();
        jogoService.responder(aluno.getId(), missao.getDesafios().get(0).getId(), CORRETA);
        jogoService.responder(aluno.getId(), missao.getDesafios().get(1).getId(), CORRETA);

        ResultadoResposta reacerto = jogoService.responder(aluno.getId(), missao.getDesafios().get(1).getId(), CORRETA);
        ResultadoResposta erro = jogoService.responder(aluno.getId(), missao.getDesafios().get(0).getId(), 0);

        assertFalse(reacerto.missaoConcluida());
        assertFalse(erro.missaoConcluida());
        assertEquals(1, eventosDoAluno(MissaoConcluida.TIPO).size());
    }
}
