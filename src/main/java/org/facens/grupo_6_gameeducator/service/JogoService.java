package org.facens.grupo_6_gameeducator.service;

import java.util.ArrayList;
import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Desafio;
import org.facens.grupo_6_gameeducator.domain.Matricula;
import org.facens.grupo_6_gameeducator.domain.Medalha;
import org.facens.grupo_6_gameeducator.domain.ProgressoAluno;
import org.facens.grupo_6_gameeducator.domain.Tentativa;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.exception.AcessoNegadoException;
import org.facens.grupo_6_gameeducator.exception.RecursoNaoEncontradoException;
import org.facens.grupo_6_gameeducator.exception.RegraDeNegocioException;
import org.facens.grupo_6_gameeducator.repository.CursoRepository;
import org.facens.grupo_6_gameeducator.repository.DesafioRepository;
import org.facens.grupo_6_gameeducator.repository.MatriculaRepository;
import org.facens.grupo_6_gameeducator.repository.MedalhaRepository;
import org.facens.grupo_6_gameeducator.repository.ProgressoAlunoRepository;
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

    /** Marcos de XP que concedem uma medalha ao aluno, no curso. */
    private static final List<Integer> MARCOS_XP = List.of(50, 100, 250, 500);

    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final DesafioRepository desafioRepository;
    private final MatriculaRepository matriculaRepository;
    private final TentativaRepository tentativaRepository;
    private final ProgressoAlunoRepository progressoAlunoRepository;
    private final MedalhaRepository medalhaRepository;

    public JogoService(UsuarioRepository usuarioRepository,
                       CursoRepository cursoRepository,
                       DesafioRepository desafioRepository,
                       MatriculaRepository matriculaRepository,
                       TentativaRepository tentativaRepository,
                       ProgressoAlunoRepository progressoAlunoRepository,
                       MedalhaRepository medalhaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.cursoRepository = cursoRepository;
        this.desafioRepository = desafioRepository;
        this.matriculaRepository = matriculaRepository;
        this.tentativaRepository = tentativaRepository;
        this.progressoAlunoRepository = progressoAlunoRepository;
        this.medalhaRepository = medalhaRepository;
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
        int xpGanho = (correta && !jaTinhaAcertado) ? desafio.getXp() : 0;

        ProgressoAluno progresso = progressoDoAluno(aluno, curso);
        int xpAntes = progresso.getXpTotal();
        progresso.creditarXp(xpGanho);
        progressoAlunoRepository.save(progresso);
        int xpDepois = progresso.getXpTotal();

        tentativaRepository.save(new Tentativa(aluno, desafio, indiceResposta, correta, xpGanho));

        List<Integer> medalhasConquistadas = concederMedalhas(aluno, curso, xpAntes, xpDepois);

        return new ResultadoResposta(correta, xpGanho, xpDepois, medalhasConquistadas);
    }

    /** Concede uma medalha para cada marco de XP cruzado entre xpAntes (exclusive) e xpDepois (inclusive). */
    private List<Integer> concederMedalhas(Usuario aluno, Curso curso, int xpAntes, int xpDepois) {
        List<Integer> conquistadas = new ArrayList<>();
        for (int marco : MARCOS_XP) {
            boolean cruzouOMarco = xpAntes < marco && xpDepois >= marco;
            if (cruzouOMarco
                    && !medalhaRepository.existsByAlunoIdAndCursoIdAndMarcoXp(aluno.getId(), curso.getId(), marco)) {
                medalhaRepository.save(new Medalha(aluno, curso, marco));
                conquistadas.add(marco);
            }
        }
        return conquistadas;
    }

    /** XP acumulado do aluno em um curso. Zero enquanto ele nao pontuou. */
    @Transactional(readOnly = true)
    public int xpNoCurso(Long alunoId, Long cursoId) {
        return progressoAlunoRepository.findByAlunoIdAndCursoId(alunoId, cursoId)
                .map(ProgressoAluno::getXpTotal)
                .orElse(0);
    }

    /** Historico de tentativas do aluno em um desafio, da mais recente para a mais antiga. */
    @Transactional(readOnly = true)
    public List<Tentativa> historico(Long alunoId, Long desafioId) {
        return tentativaRepository.findByAlunoIdAndDesafioIdOrderByDataHoraDesc(alunoId, desafioId);
    }

    /** Ranking de XP da turma, do maior para o menor. */
    @Transactional(readOnly = true)
    public List<ProgressoAluno> ranking(Long cursoId) {
        return progressoAlunoRepository.findByCursoIdOrderByXpTotalDesc(cursoId);
    }

    /** Medalhas conquistadas pelo aluno naquele curso, do menor para o maior marco. */
    @Transactional(readOnly = true)
    public List<Medalha> medalhasDoAluno(Long alunoId, Long cursoId) {
        return medalhaRepository.findByAlunoIdAndCursoIdOrderByMarcoXpAsc(alunoId, cursoId);
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

    private ProgressoAluno progressoDoAluno(Usuario aluno, Curso curso) {
        return progressoAlunoRepository.findByAlunoIdAndCursoId(aluno.getId(), curso.getId())
                .orElseGet(() -> new ProgressoAluno(aluno, curso));
    }
}
