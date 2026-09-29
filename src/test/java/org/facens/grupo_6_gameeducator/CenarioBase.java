package org.facens.grupo_6_gameeducator;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Matricula;
import org.facens.grupo_6_gameeducator.domain.Papel;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.repository.CursoRepository;
import org.facens.grupo_6_gameeducator.repository.MatriculaRepository;
import org.facens.grupo_6_gameeducator.repository.MedalhaRepository;
import org.facens.grupo_6_gameeducator.repository.MissaoRepository;
import org.facens.grupo_6_gameeducator.repository.NotaRepository;
import org.facens.grupo_6_gameeducator.repository.PostRepository;
import org.facens.grupo_6_gameeducator.repository.ProgressoAlunoRepository;
import org.facens.grupo_6_gameeducator.repository.TentativaRepository;
import org.facens.grupo_6_gameeducator.repository.UsuarioRepository;
import org.facens.grupo_6_gameeducator.service.ForumService;
import org.facens.grupo_6_gameeducator.service.JogoService;
import org.facens.grupo_6_gameeducator.service.NotaService;
import org.facens.grupo_6_gameeducator.service.MissaoService;
import org.facens.grupo_6_gameeducator.service.dto.NovaMissaoRequest;
import org.facens.grupo_6_gameeducator.service.dto.NovoDesafioRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Setup ("fixture") compartilhado pelos cenarios do Sprint Backlog.
 * Cada teste roda em uma transacao propria, revertida no fim.
 */
@SpringBootTest
@Transactional
abstract class CenarioBase {

    private static final AtomicInteger SEQUENCIA = new AtomicInteger();

    @Autowired
    protected UsuarioRepository usuarioRepository;
    @Autowired
    protected CursoRepository cursoRepository;
    @Autowired
    protected MatriculaRepository matriculaRepository;
    @Autowired
    protected MissaoRepository missaoRepository;
    @Autowired
    protected TentativaRepository tentativaRepository;
    @Autowired
    protected ProgressoAlunoRepository progressoAlunoRepository;
    @Autowired
    protected MedalhaRepository medalhaRepository;
    @Autowired
    protected NotaRepository notaRepository;
    @Autowired
    protected PostRepository postRepository;

    @Autowired
    protected MissaoService missaoService;
    @Autowired
    protected JogoService jogoService;
    @Autowired
    protected NotaService notaService;
    @Autowired
    protected ForumService forumService;

    protected Usuario umProfessor(String nome) {
        return usuarioRepository.save(new Usuario(nome, email(nome), Papel.PROFESSOR));
    }

    protected Usuario umAluno(String nome) {
        return usuarioRepository.save(new Usuario(nome, email(nome), Papel.ALUNO));
    }

    protected Curso umCurso(String titulo, Usuario professor) {
        return cursoRepository.save(new Curso(titulo, professor));
    }

    protected Matricula matricular(Usuario aluno, Curso curso) {
        return matriculaRepository.save(new Matricula(curso, aluno));
    }

    /** Missao de exemplo com um desafio de 4 alternativas, resposta correta no indice 1. */
    protected NovaMissaoRequest missaoDeExemplo(int xp) {
        return new NovaMissaoRequest(
                "Missao 1 - Fracoes",
                "Resolva os desafios sobre fracoes equivalentes",
                List.of(new NovoDesafioRequest(
                        "Quanto e 1/2 + 1/4 ?",
                        List.of("1/6", "3/4", "2/6", "1/8"),
                        1,
                        xp)));
    }

    private static String email(String nome) {
        return nome.toLowerCase().replaceAll("[^a-z]", "") + SEQUENCIA.incrementAndGet() + "@facens.br";
    }
}
