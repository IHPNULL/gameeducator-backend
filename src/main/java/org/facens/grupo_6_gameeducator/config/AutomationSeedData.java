package org.facens.grupo_6_gameeducator.config;

import java.math.BigDecimal;
import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Matricula;
import org.facens.grupo_6_gameeducator.domain.Missao;
import org.facens.grupo_6_gameeducator.domain.Papel;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.repository.CursoRepository;
import org.facens.grupo_6_gameeducator.repository.MatriculaRepository;
import org.facens.grupo_6_gameeducator.repository.UsuarioRepository;
import org.facens.grupo_6_gameeducator.service.ForumService;
import org.facens.grupo_6_gameeducator.service.JogoService;
import org.facens.grupo_6_gameeducator.service.MissaoService;
import org.facens.grupo_6_gameeducator.service.NotaService;
import org.facens.grupo_6_gameeducator.service.dto.NovaMissaoRequest;
import org.facens.grupo_6_gameeducator.service.dto.NovoDesafioRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Popula o banco (H2 em memoria, ver application-automation.properties) com um cenario
 * fixo e conhecido para a suite de automacao Cucumber em automation/. Ativo somente no
 * profile "automation": nunca roda em dev/producao nem nos testes unitarios do backend.
 *
 * <p>Os IDs nascem previsiveis (schema recriado do zero a cada start, sequencia comecando
 * em 1): professor=1, alunoUm=2, alunoDois=3, curso=1, missao=1, desafios=1 e 2. As features
 * Gherkin em automation/api-tests e automation/ui-tests dependem desses IDs fixos.
 */
@Component
@Profile("automation")
public class AutomationSeedData implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AutomationSeedData.class);

    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final MatriculaRepository matriculaRepository;
    private final MissaoService missaoService;
    private final JogoService jogoService;
    private final NotaService notaService;
    private final ForumService forumService;

    public AutomationSeedData(UsuarioRepository usuarioRepository,
                              CursoRepository cursoRepository,
                              MatriculaRepository matriculaRepository,
                              MissaoService missaoService,
                              JogoService jogoService,
                              NotaService notaService,
                              ForumService forumService) {
        this.usuarioRepository = usuarioRepository;
        this.cursoRepository = cursoRepository;
        this.matriculaRepository = matriculaRepository;
        this.missaoService = missaoService;
        this.jogoService = jogoService;
        this.notaService = notaService;
        this.forumService = forumService;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        Usuario professor = usuarioRepository.save(
                new Usuario("Professor Automacao", "professor.automacao@gameeducator.test", Papel.PROFESSOR));
        Usuario alunoUm = usuarioRepository.save(
                new Usuario("Aluno Um Automacao", "aluno.um.automacao@gameeducator.test", Papel.ALUNO));
        Usuario alunoDois = usuarioRepository.save(
                new Usuario("Aluno Dois Automacao", "aluno.dois.automacao@gameeducator.test", Papel.ALUNO));

        Curso curso = cursoRepository.save(new Curso("Curso de Automacao", professor));
        matriculaRepository.save(new Matricula(curso, alunoUm));
        matriculaRepository.save(new Matricula(curso, alunoDois));

        Missao missao = missaoService.criar(professor.getId(), curso.getId(), new NovaMissaoRequest(
                "Missao Seed de Automacao",
                "Cenario fixo usado pela suite de automacao Cucumber",
                List.of(
                        new NovoDesafioRequest("Quanto e 2 + 2 ?",
                                List.of("3", "4", "5", "6"), 1, 10),
                        new NovoDesafioRequest("Qual e a capital do Brasil ?",
                                List.of("Rio de Janeiro", "Brasilia", "Sao Paulo"), 1, 15))));

        // Aluno Um ja respondeu o primeiro desafio, ja tem nota e ja abriu um topico no
        // forum: assim as telas de leitura (ranking, notas, forum) tem conteudo desde
        // o primeiro GET, sem cada cenario precisar criar dados antes de poder ler.
        jogoService.responder(alunoUm.getId(), missao.getDesafios().get(0).getId(), 1);
        notaService.lancar(professor.getId(), curso.getId(), alunoUm.getId(), "Prova 1", new BigDecimal("8.5"));
        forumService.publicar(alunoUm.getId(), curso.getId(), "Duvida inicial",
                "Alguem pode ajudar com o desafio 1 da missao?");

        log.info("""

                ==================== DADOS DE AUTOMACAO (profile automation) ====================
                 Professor  {}   id={}
                 Aluno      {}   id={}  (matriculado, ja respondeu o desafio 1)
                 Aluno      {}   id={}  (matriculado, sem respostas ainda)
                 Curso      id={}  "{}"
                 Missao     id={}  com {} desafios (XP total {})
                 Desafios   ids={}
                ===================================================================================
                """,
                professor.getNome(), professor.getId(),
                alunoUm.getNome(), alunoUm.getId(),
                alunoDois.getNome(), alunoDois.getId(),
                curso.getId(), curso.getTitulo(),
                missao.getId(), missao.getDesafios().size(), missao.getXpTotal(),
                missao.getDesafios().stream().map(d -> String.valueOf(d.getId())).toList());
    }
}
