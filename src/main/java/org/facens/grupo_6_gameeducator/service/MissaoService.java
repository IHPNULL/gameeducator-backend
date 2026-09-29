package org.facens.grupo_6_gameeducator.service;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Desafio;
import org.facens.grupo_6_gameeducator.domain.Missao;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.exception.AcessoNegadoException;
import org.facens.grupo_6_gameeducator.exception.RecursoNaoEncontradoException;
import org.facens.grupo_6_gameeducator.exception.RegraDeNegocioException;
import org.facens.grupo_6_gameeducator.repository.CursoRepository;
import org.facens.grupo_6_gameeducator.repository.MatriculaRepository;
import org.facens.grupo_6_gameeducator.repository.MissaoRepository;
import org.facens.grupo_6_gameeducator.repository.UsuarioRepository;
import org.facens.grupo_6_gameeducator.service.dto.NovaMissaoRequest;
import org.facens.grupo_6_gameeducator.service.dto.NovoDesafioRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US02 - Como professor, quero criar missoes com desafios dentro de um curso,
 * para disponibilizar o conteudo do curso de forma gamificada.
 */
@Service
@Transactional
public class MissaoService {

    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final MissaoRepository missaoRepository;
    private final MatriculaRepository matriculaRepository;

    public MissaoService(UsuarioRepository usuarioRepository,
                         CursoRepository cursoRepository,
                         MissaoRepository missaoRepository,
                         MatriculaRepository matriculaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.cursoRepository = cursoRepository;
        this.missaoRepository = missaoRepository;
        this.matriculaRepository = matriculaRepository;
    }

    /** Cria a missao com seus desafios. So o professor responsavel pelo curso pode fazer isso. */
    public Missao criar(Long professorId, Long cursoId, NovaMissaoRequest request) {
        Usuario professor = usuarioRepository.findById(professorId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Usuario", professorId));
        if (!professor.isProfessor()) {
            throw new AcessoNegadoException("Somente um professor pode criar missoes");
        }

        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Curso", cursoId));
        if (!curso.ehResponsavel(professor)) {
            throw new AcessoNegadoException("Professor nao e responsavel pelo curso " + cursoId);
        }

        validar(request);

        Missao missao = new Missao(request.titulo(), request.descricao(), curso);
        for (NovoDesafioRequest novoDesafio : request.desafios()) {
            missao.adicionarDesafio(new Desafio(
                    novoDesafio.enunciado(),
                    novoDesafio.alternativas(),
                    novoDesafio.indiceRespostaCorreta(),
                    novoDesafio.xp()));
        }
        return missaoRepository.save(missao);
    }

    /** Lista as missoes de um curso conforme o papel do usuario autenticado. */
    @Transactional(readOnly = true)
    public List<Missao> listarParaUsuario(Long usuarioId, Long cursoId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Usuario", usuarioId));
        return usuario.isProfessor()
                ? listarParaProfessor(usuarioId, cursoId)
                : listarParaAluno(usuarioId, cursoId);
    }

    /** Missoes que o aluno enxerga em um curso. Exige matricula ativa. */
    @Transactional(readOnly = true)
    public List<Missao> listarParaAluno(Long alunoId, Long cursoId) {
        if (!matriculaRepository.existsByCursoIdAndAlunoId(cursoId, alunoId)) {
            throw new AcessoNegadoException("Aluno " + alunoId + " nao esta matriculado no curso " + cursoId);
        }
        return missaoRepository.findByCursoIdOrderByIdAsc(cursoId);
    }

    /** Missoes de um curso sob a otica do professor responsavel. */
    @Transactional(readOnly = true)
    public List<Missao> listarParaProfessor(Long professorId, Long cursoId) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Curso", cursoId));
        if (!curso.getProfessor().getId().equals(professorId)) {
            throw new AcessoNegadoException("Professor nao e responsavel pelo curso " + cursoId);
        }
        return missaoRepository.findByCursoIdOrderByIdAsc(cursoId);
    }

    private void validar(NovaMissaoRequest request) {
        if (request.titulo() == null || request.titulo().isBlank()) {
            throw new RegraDeNegocioException("A missao precisa de um titulo");
        }
        if (request.desafios() == null || request.desafios().isEmpty()) {
            throw new RegraDeNegocioException("A missao precisa de ao menos um desafio");
        }
        for (NovoDesafioRequest desafio : request.desafios()) {
            if (desafio.enunciado() == null || desafio.enunciado().isBlank()) {
                throw new RegraDeNegocioException("Todo desafio precisa de um enunciado");
            }
            if (desafio.alternativas() == null || desafio.alternativas().size() < 2) {
                throw new RegraDeNegocioException("Todo desafio precisa de ao menos duas alternativas");
            }
            if (desafio.indiceRespostaCorreta() < 0 || desafio.indiceRespostaCorreta() >= desafio.alternativas().size()) {
                throw new RegraDeNegocioException("A resposta correta precisa apontar para uma alternativa existente");
            }
            if (desafio.xp() <= 0) {
                throw new RegraDeNegocioException("O XP do desafio precisa ser maior que zero");
            }
        }
    }
}
