package org.facens.grupo_6_gameeducator.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Desafio;
import org.facens.grupo_6_gameeducator.domain.Matricula;
import org.facens.grupo_6_gameeducator.domain.Missao;
import org.facens.grupo_6_gameeducator.domain.Tentativa;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.eventos.EmissorDeEventos;
import org.facens.grupo_6_gameeducator.eventos.MedalhaConquistada;
import org.facens.grupo_6_gameeducator.eventos.MissaoConcluida;
import org.facens.grupo_6_gameeducator.exception.AcessoNegadoException;
import org.facens.grupo_6_gameeducator.exception.RecursoNaoEncontradoException;
import org.facens.grupo_6_gameeducator.exception.RegraDeNegocioException;
import org.facens.grupo_6_gameeducator.gamificacao.GamificacaoApi;
import org.facens.grupo_6_gameeducator.gamificacao.RespostaPontuavel;
import org.facens.grupo_6_gameeducator.gamificacao.ResultadoPontuacao;
import org.facens.grupo_6_gameeducator.repository.CursoRepository;
import org.facens.grupo_6_gameeducator.repository.DesafioRepository;
import org.facens.grupo_6_gameeducator.repository.MatriculaRepository;
import org.facens.grupo_6_gameeducator.repository.TentativaRepository;
import org.facens.grupo_6_gameeducator.repository.UsuarioRepository;
import org.facens.grupo_6_gameeducator.service.dto.DesempenhoAluno;
import org.facens.grupo_6_gameeducator.service.dto.ResultadoResposta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US01 - Como aluno, quero resolver os desafios de uma missao e receber XP por acerto,
 * para aprender jogando e saber o meu progresso.
 */
@Service
@Transactional
public class JogoService {

    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final DesafioRepository desafioRepository;
    private final MatriculaRepository matriculaRepository;
    private final TentativaRepository tentativaRepository;
    private final GamificacaoApi gamificacao;
    private final EmissorDeEventos eventos;

    public JogoService(UsuarioRepository usuarioRepository,
                       CursoRepository cursoRepository,
                       DesafioRepository desafioRepository,
                       MatriculaRepository matriculaRepository,
                       TentativaRepository tentativaRepository,
                       GamificacaoApi gamificacao,
                       EmissorDeEventos eventos) {
        this.usuarioRepository = usuarioRepository;
        this.cursoRepository = cursoRepository;
        this.desafioRepository = desafioRepository;
        this.matriculaRepository = matriculaRepository;
        this.tentativaRepository = tentativaRepository;
        this.gamificacao = gamificacao;
        this.eventos = eventos;
    }

    /**
     * Registra a resposta do aluno a um desafio e credita o XP quando ele acerta.
     * O XP de um desafio e creditado uma unica vez: reacertar o mesmo desafio nao acumula XP.
     */
    public ResultadoResposta responder(Long alunoId, Long desafioId, int indiceResposta) {
        Usuario aluno = usuarioRepository.findById(alunoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Usuario", alunoId));
        if (!aluno.isAluno()) {
            throw new AcessoNegadoException("Somente um aluno pode responder desafios");
        }

        Desafio desafio = desafioRepository.findById(desafioId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Desafio", desafioId));
        if (indiceResposta < 0 || indiceResposta >= desafio.getAlternativas().size()) {
            throw new RegraDeNegocioException("Alternativa inexistente para o desafio " + desafioId);
        }

        Curso curso = desafio.getMissao().getCurso();
        if (!matriculaRepository.existsByCursoIdAndAlunoId(curso.getId(), alunoId)) {
            throw new AcessoNegadoException("Aluno " + alunoId + " nao esta matriculado no curso " + curso.getId());
        }

        boolean correta = desafio.estaCorreta(indiceResposta);
        boolean jaTinhaAcertado = tentativaRepository.existsByAlunoIdAndDesafioIdAndCorretaTrue(alunoId, desafioId);

        // XP e medalhas sao regras do modulo de gamificacao (ADR-B1-001/002): o jogo so informa o fato.
        ResultadoPontuacao pontuacao = gamificacao.pontuarResposta(
                new RespostaPontuavel(alunoId, curso.getId(), desafio.getXp(), correta, jaTinhaAcertado));

        tentativaRepository.save(new Tentativa(aluno, desafio, indiceResposta, correta, pontuacao.xpGanho()));

        // Desafio 4: o que e eventual (notificacao/analytics) sai como evento na MESMA transacao (outbox).
        Instant agora = Instant.now();
        for (int marco : pontuacao.medalhasConquistadas()) {
            eventos.emitir(new MedalhaConquistada(UUID.randomUUID().toString(), alunoId, curso.getId(), marco, agora));
        }
        boolean missaoConcluida = correta && !jaTinhaAcertado && concluiuAMissao(alunoId, desafio.getMissao());
        if (missaoConcluida) {
            eventos.emitir(new MissaoConcluida(UUID.randomUUID().toString(), alunoId, curso.getId(),
                    desafio.getMissao().getId(), desafio.getMissao().getTitulo(), pontuacao.xpTotalNoCurso(), agora));
        }

        return new ResultadoResposta(correta, pontuacao.xpGanho(), pontuacao.xpTotalNoCurso(),
                pontuacao.medalhasConquistadas(), missaoConcluida);
    }

    /** A missao esta concluida quando o aluno tem ao menos um acerto em cada desafio dela. */
    private boolean concluiuAMissao(Long alunoId, Missao missao) {
        return tentativaRepository.contarDesafiosAcertadosNaMissao(alunoId, missao.getId()) == missao.getDesafios().size();
    }

    /** XP acumulado do aluno em um curso. Zero enquanto ele nao pontuou. */
    @Transactional(readOnly = true)
    public int xpNoCurso(Long alunoId, Long cursoId) {
        return gamificacao.xpNoCurso(alunoId, cursoId);
    }

    /** Historico de tentativas do aluno em um desafio, da mais recente para a mais antiga. */
    @Transactional(readOnly = true)
    public List<Tentativa> historico(Long alunoId, Long desafioId) {
        return tentativaRepository.findByAlunoIdAndDesafioIdOrderByDataHoraDesc(alunoId, desafioId);
    }

    /** Desempenho da turma no curso: XP, tentativas e acertos de cada aluno matriculado. So o professor responsavel ve. */
    @Transactional(readOnly = true)
    public List<DesempenhoAluno> desempenhoDaTurma(Long professorId, Long cursoId) {
        Usuario professor = usuarioRepository.findById(professorId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Usuario", professorId));
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Curso", cursoId));
        if (!curso.ehResponsavel(professor)) {
            throw new AcessoNegadoException("Somente o professor responsavel acompanha a turma do curso " + cursoId);
        }

        return matriculaRepository.findByCursoId(cursoId).stream()
                .map(Matricula::getAluno)
                // aluno vem de uma associacao LAZY: forca a inicializacao aqui, enquanto a sessao
                // ainda esta aberta, pois o controller le aluno.getNome() ja fora da transacao.
                .peek(Usuario::getNome)
                .map(aluno -> desempenhoDoAluno(aluno, cursoId))
                .toList();
    }

    private DesempenhoAluno desempenhoDoAluno(Usuario aluno, Long cursoId) {
        int xpTotal = xpNoCurso(aluno.getId(), cursoId);
        List<Tentativa> tentativas = tentativaRepository.findByAlunoIdAndDesafio_Missao_Curso_Id(aluno.getId(), cursoId);
        int acertos = (int) tentativas.stream().filter(Tentativa::isCorreta).count();
        return new DesempenhoAluno(aluno, xpTotal, tentativas.size(), acertos);
    }
}
