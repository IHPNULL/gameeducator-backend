package org.facens.grupo_6_gameeducator.gamificacao.aplicacao;

import java.util.List;
import org.facens.grupo_6_gameeducator.gamificacao.GamificacaoApi;
import org.facens.grupo_6_gameeducator.gamificacao.MedalhaDoAluno;
import org.facens.grupo_6_gameeducator.gamificacao.PosicaoNoRanking;
import org.facens.grupo_6_gameeducator.gamificacao.RespostaPontuavel;
import org.facens.grupo_6_gameeducator.gamificacao.ResultadoPontuacao;
import org.facens.grupo_6_gameeducator.gamificacao.dominio.PoliticaDeMedalhas;
import org.facens.grupo_6_gameeducator.gamificacao.dominio.PoliticaDeXp;
import org.facens.grupo_6_gameeducator.gamificacao.dominio.Progresso;
import org.facens.grupo_6_gameeducator.gamificacao.porta.RepositorioDeMedalhas;
import org.facens.grupo_6_gameeducator.gamificacao.porta.RepositorioDeProgresso;

/**
 * Caso de uso da gamificacao. Sem anotacoes de Spring nem JPA: depende so do dominio e das
 * portas, e por isso roda em teste com adaptadores em memoria, sem subir contexto nem banco.
 * A transacao e a do chamador (JogoService.responder e {@code @Transactional}).
 */
public class GamificacaoService implements GamificacaoApi {

    private final RepositorioDeProgresso progressos;
    private final RepositorioDeMedalhas medalhas;
    private final PoliticaDeMedalhas politicaDeMedalhas;

    public GamificacaoService(RepositorioDeProgresso progressos,
                              RepositorioDeMedalhas medalhas,
                              PoliticaDeMedalhas politicaDeMedalhas) {
        this.progressos = progressos;
        this.medalhas = medalhas;
        this.politicaDeMedalhas = politicaDeMedalhas;
    }

    @Override
    public ResultadoPontuacao pontuarResposta(RespostaPontuavel resposta) {
        int xpGanho = PoliticaDeXp.xpACreditar(resposta.correta(), resposta.jaHaviaAcertado(), resposta.xpDoDesafio());

        Progresso antes = progressos.carregar(resposta.alunoId(), resposta.cursoId());
        Progresso depois = antes.creditar(xpGanho);
        progressos.salvar(depois);

        List<Integer> novas = politicaDeMedalhas.novasMedalhas(
                antes.xpTotal(), depois.xpTotal(),
                medalhas.marcosConquistados(resposta.alunoId(), resposta.cursoId()));
        novas.forEach(marco -> medalhas.registrar(resposta.alunoId(), resposta.cursoId(), marco));

        return new ResultadoPontuacao(xpGanho, depois.xpTotal(), novas);
    }

    @Override
    public int xpNoCurso(Long alunoId, Long cursoId) {
        return progressos.carregar(alunoId, cursoId).xpTotal();
    }

    @Override
    public List<PosicaoNoRanking> ranking(Long cursoId) {
        return progressos.ranking(cursoId);
    }

    @Override
    public List<MedalhaDoAluno> medalhasDoAluno(Long alunoId, Long cursoId) {
        return medalhas.listar(alunoId, cursoId);
    }
}
