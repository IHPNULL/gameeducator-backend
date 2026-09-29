package org.facens.grupo_6_gameeducator.config;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Matricula;
import org.facens.grupo_6_gameeducator.domain.Missao;
import org.facens.grupo_6_gameeducator.domain.Papel;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.repository.CursoRepository;
import org.facens.grupo_6_gameeducator.repository.MatriculaRepository;
import org.facens.grupo_6_gameeducator.repository.UsuarioRepository;
import org.facens.grupo_6_gameeducator.service.JogoService;
import org.facens.grupo_6_gameeducator.service.MissaoService;
import org.facens.grupo_6_gameeducator.service.dto.NovaMissaoRequest;
import org.facens.grupo_6_gameeducator.service.dto.NovoDesafioRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Popula o banco em memoria com um cenario navegavel.
 * Ativo somente no profile "dev": nao roda em producao nem nos testes.
 */
@Component
@Profile("dev")
public class DadosDeExemplo implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosDeExemplo.class);

    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final MatriculaRepository matriculaRepository;
    private final MissaoService missaoService;
    private final JogoService jogoService;

    public DadosDeExemplo(UsuarioRepository usuarioRepository,
                          CursoRepository cursoRepository,
                          MatriculaRepository matriculaRepository,
                          MissaoService missaoService,
                          JogoService jogoService) {
        this.usuarioRepository = usuarioRepository;
        this.cursoRepository = cursoRepository;
        this.matriculaRepository = matriculaRepository;
        this.missaoService = missaoService;
        this.jogoService = jogoService;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        Usuario ana = usuarioRepository.save(new Usuario("Ana", "ana@facens.br", Papel.PROFESSOR));
        Usuario bruno = usuarioRepository.save(new Usuario("Bruno", "bruno@facens.br", Papel.ALUNO));
        Usuario daniela = usuarioRepository.save(new Usuario("Daniela", "daniela@facens.br", Papel.ALUNO));

        Curso curso = cursoRepository.save(new Curso("Matematica Basica", ana));
        matriculaRepository.save(new Matricula(curso, bruno));
        matriculaRepository.save(new Matricula(curso, daniela));

        Missao missao = missaoService.criar(ana.getId(), curso.getId(), new NovaMissaoRequest(
                "Missao 1 - Fracoes",
                "Resolva os desafios sobre fracoes equivalentes",
                List.of(
                        new NovoDesafioRequest("Quanto e 1/2 + 1/4 ?",
                                List.of("1/6", "3/4", "2/6", "1/8"), 1, 10),
                        new NovoDesafioRequest("1/3 e equivalente a ?",
                                List.of("2/6", "3/4", "1/4"), 0, 15),
                        new NovoDesafioRequest("Qual fracao e maior que 1 ?",
                                List.of("2/3", "5/4", "1/2"), 1, 20))));

        // Daniela ja jogou os dois primeiros desafios para o ranking nascer com conteudo
        jogoService.responder(daniela.getId(), missao.getDesafios().get(0).getId(), 1);
        jogoService.responder(daniela.getId(), missao.getDesafios().get(1).getId(), 0);

        log.info("""

                ==================== DADOS DE EXEMPLO (profile dev) ====================
                 Professor  Ana      id={}  (responsavel pelo curso)
                 Aluno      Bruno    id={}  (matriculado, 0 XP)
                 Aluno      Daniela  id={}  (matriculado, 25 XP)
                 Curso      id={}  "{}"
                 Missao     id={}  com {} desafios (XP total {})
                 Desafios   ids={}
                ========================================================================
                """,
                ana.getId(), bruno.getId(), daniela.getId(),
                curso.getId(), curso.getTitulo(),
                missao.getId(), missao.getDesafios().size(), missao.getXpTotal(),
                missao.getDesafios().stream().map(d -> String.valueOf(d.getId())).toList());
    }
}
