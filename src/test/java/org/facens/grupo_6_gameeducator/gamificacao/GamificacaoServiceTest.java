package org.facens.grupo_6_gameeducator.gamificacao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.facens.grupo_6_gameeducator.gamificacao.adaptador.memoria.GamificacaoEmMemoria;
import org.facens.grupo_6_gameeducator.gamificacao.aplicacao.GamificacaoService;
import org.facens.grupo_6_gameeducator.gamificacao.dominio.PoliticaDeMedalhas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Desafio 2 - o caso de uso de gamificacao roda com o adaptador em memoria: sem Spring e sem JPA.
 * Os mesmos cenarios de AlunoGanhaMedalhaTest (que usam JPA + H2) passam aqui em milissegundos.
 */
@DisplayName("Caso de uso de gamificacao com adaptador em memoria")
class GamificacaoServiceTest {

    private static final long ALUNO = 2L;
    private static final long CURSO = 10L;

    private GamificacaoEmMemoria memoria;
    private GamificacaoApi gamificacao;

    @BeforeEach
    void montar() {
        memoria = new GamificacaoEmMemoria();
        gamificacao = new GamificacaoService(memoria, memoria, PoliticaDeMedalhas.padrao());
    }

    @Test
    @DisplayName("acerto credita XP e concede a medalha do marco cruzado")
    void acertoCreditaXpEMedalha() {
        ResultadoPontuacao resultado = gamificacao.pontuarResposta(new RespostaPontuavel(ALUNO, CURSO, 50, true, false));

        assertEquals(50, resultado.xpGanho());
        assertEquals(50, resultado.xpTotalNoCurso());
        assertEquals(List.of(50), resultado.medalhasConquistadas());
        assertEquals(50, gamificacao.xpNoCurso(ALUNO, CURSO));
        assertEquals(50, gamificacao.medalhasDoAluno(ALUNO, CURSO).get(0).marcoXp());
    }

    @Test
    @DisplayName("reacerto nao credita XP nem repete medalha")
    void reacertoNaoPontua() {
        gamificacao.pontuarResposta(new RespostaPontuavel(ALUNO, CURSO, 50, true, false));
        ResultadoPontuacao repeticao = gamificacao.pontuarResposta(new RespostaPontuavel(ALUNO, CURSO, 50, true, true));

        assertEquals(0, repeticao.xpGanho());
        assertEquals(List.of(), repeticao.medalhasConquistadas());
        assertEquals(1, gamificacao.medalhasDoAluno(ALUNO, CURSO).size());
    }

    @Test
    @DisplayName("ranking ordena a turma por XP e usa o nome registrado")
    void rankingOrdenado() {
        memoria.registrarNome(3L, "Daniela");
        gamificacao.pontuarResposta(new RespostaPontuavel(ALUNO, CURSO, 10, true, false));
        gamificacao.pontuarResposta(new RespostaPontuavel(3L, CURSO, 30, true, false));
        gamificacao.pontuarResposta(new RespostaPontuavel(4L, 99L, 80, true, false));

        List<PosicaoNoRanking> ranking = gamificacao.ranking(CURSO);

        assertEquals(List.of(
                new PosicaoNoRanking(1, 3L, "Daniela", 30),
                new PosicaoNoRanking(2, ALUNO, "Aluno 2", 10)), ranking);
    }
}
