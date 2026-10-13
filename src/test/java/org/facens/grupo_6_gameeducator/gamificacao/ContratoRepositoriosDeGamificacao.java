package org.facens.grupo_6_gameeducator.gamificacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.facens.grupo_6_gameeducator.gamificacao.dominio.Progresso;
import org.facens.grupo_6_gameeducator.gamificacao.porta.RepositorioDeMedalhas;
import org.facens.grupo_6_gameeducator.gamificacao.porta.RepositorioDeProgresso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Desafio 2 - contrato das portas de saida. O MESMO conjunto de testes roda contra o adaptador
 * JPA e contra o adaptador em memoria: e isso que torna a troca de adaptador segura.
 */
abstract class ContratoRepositoriosDeGamificacao {

    protected abstract RepositorioDeProgresso progressos();

    protected abstract RepositorioDeMedalhas medalhas();

    /** Cria um aluno no "mundo" do adaptador e devolve o id. */
    protected abstract Long novoAluno(String nome);

    /** Cria um curso no "mundo" do adaptador e devolve o id. */
    protected abstract Long novoCurso();

    @Test
    @DisplayName("contrato: aluno sem pontuacao tem progresso inicial 0")
    void progressoInicial() {
        Long aluno = novoAluno("Bruno");
        Long curso = novoCurso();

        assertEquals(Progresso.inicial(aluno, curso), progressos().carregar(aluno, curso));
    }

    @Test
    @DisplayName("contrato: salvar e carregar devolvem o mesmo progresso (inclusive na atualizacao)")
    void salvarECarregar() {
        Long aluno = novoAluno("Bruno");
        Long curso = novoCurso();

        progressos().salvar(new Progresso(aluno, curso, 20));
        progressos().salvar(new Progresso(aluno, curso, 35));

        assertEquals(35, progressos().carregar(aluno, curso).xpTotal());
    }

    @Test
    @DisplayName("contrato: ranking traz so a turma, do maior XP para o menor, com posicao e nome")
    void ranking() {
        Long curso = novoCurso();
        Long outroCurso = novoCurso();
        Long bruno = novoAluno("Bruno");
        Long daniela = novoAluno("Daniela");
        progressos().salvar(new Progresso(bruno, curso, 10));
        progressos().salvar(new Progresso(daniela, curso, 40));
        progressos().salvar(new Progresso(bruno, outroCurso, 99));

        assertEquals(List.of(
                new PosicaoNoRanking(1, daniela, "Daniela", 40),
                new PosicaoNoRanking(2, bruno, "Bruno", 10)), progressos().ranking(curso));
    }

    @Test
    @DisplayName("contrato: medalhas registradas sao listadas em ordem de marco")
    void medalhasListadasEmOrdem() {
        Long aluno = novoAluno("Bruno");
        Long curso = novoCurso();
        assertTrue(medalhas().marcosConquistados(aluno, curso).isEmpty());

        medalhas().registrar(aluno, curso, 100);
        medalhas().registrar(aluno, curso, 50);

        assertEquals(Set.of(50, 100), medalhas().marcosConquistados(aluno, curso));
        assertEquals(List.of(50, 100), medalhas().listar(aluno, curso).stream().map(MedalhaDoAluno::marcoXp).toList());
    }
}
