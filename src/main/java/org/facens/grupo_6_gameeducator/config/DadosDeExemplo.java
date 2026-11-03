package org.facens.grupo_6_gameeducator.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Matricula;
import org.facens.grupo_6_gameeducator.domain.Missao;
import org.facens.grupo_6_gameeducator.domain.Papel;
import org.facens.grupo_6_gameeducator.domain.Post;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.iot.EventoDeLaboratorio;
import org.facens.grupo_6_gameeducator.iot.RegistroDeEventosIot;
import org.facens.grupo_6_gameeducator.iot.TipoEventoIot;
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
 * Popula o banco em memoria com um cenario completo para apresentacao e navegacao em dev.
 * Ativo somente no profile "dev": nao roda em producao nem nos testes automatizados.
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
    private final NotaService notaService;
    private final ForumService forumService;
    private final RegistroDeEventosIot registroDeEventosIot;

    public DadosDeExemplo(UsuarioRepository usuarioRepository,
                          CursoRepository cursoRepository,
                          MatriculaRepository matriculaRepository,
                          MissaoService missaoService,
                          JogoService jogoService,
                          NotaService notaService,
                          ForumService forumService,
                          RegistroDeEventosIot registroDeEventosIot) {
        this.usuarioRepository = usuarioRepository;
        this.cursoRepository = cursoRepository;
        this.matriculaRepository = matriculaRepository;
        this.missaoService = missaoService;
        this.jogoService = jogoService;
        this.notaService = notaService;
        this.forumService = forumService;
        this.registroDeEventosIot = registroDeEventosIot;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        // 1. Usuarios das Personas da Apresentacao
        Usuario ana = usuarioRepository.save(new Usuario("Professora Ana", "ana@facens.br", Papel.PROFESSOR));
        Usuario bruno = usuarioRepository.save(new Usuario("Bruno", "bruno@facens.br", Papel.ALUNO));
        Usuario carlos = usuarioRepository.save(new Usuario("Carlos", "carlos@facens.br", Papel.ALUNO));
        Usuario daniela = usuarioRepository.save(new Usuario("Daniela", "daniela@facens.br", Papel.ALUNO));
        Usuario eduardo = usuarioRepository.save(new Usuario("Eduardo", "eduardo@facens.br", Papel.ALUNO));

        // 2. Curso e Matriculas
        Curso curso = cursoRepository.save(new Curso("Matematica Basica e Raciocinio Logico", ana));
        matriculaRepository.save(new Matricula(curso, bruno));
        matriculaRepository.save(new Matricula(curso, carlos));
        matriculaRepository.save(new Matricula(curso, daniela));
        matriculaRepository.save(new Matricula(curso, eduardo));

        // 3. Missoes com Desafios Gamificados
        Missao missao1 = missaoService.criar(ana.getId(), curso.getId(), new NovaMissaoRequest(
                "Missao 1 - Fracoes e Proporcoes",
                "Resolva desafios sobre fracoes equivalentes, simplificacao e operacoes basicas",
                List.of(
                        new NovoDesafioRequest("Quanto e 1/2 + 1/4 ?",
                                List.of("1/6", "3/4", "2/6", "1/8"), 1, 15),
                        new NovoDesafioRequest("1/3 e equivalente a qual fracao?",
                                List.of("2/6", "3/4", "1/4", "3/6"), 0, 20),
                        new NovoDesafioRequest("Qual fracao e estritamente maior que 1 ?",
                                List.of("2/3", "5/4", "1/2", "7/8"), 1, 25))));

        Missao missao2 = missaoService.criar(ana.getId(), curso.getId(), new NovaMissaoRequest(
                "Missao 2 - Geometria Espacial",
                "Calcule areas e volumes de prismas, cilindros e analise solidos geometricos",
                List.of(
                        new NovoDesafioRequest("Qual e a formula do volume de um prisma reto de base B e altura h?",
                                List.of("V = B * h", "V = (B * h) / 3", "V = 2 * B * h", "V = B + h"), 0, 20),
                        new NovoDesafioRequest("Quantas faces possui um cubo (hexaedro regular)?",
                                List.of("4 faces", "6 faces", "8 faces", "12 faces"), 1, 25),
                        new NovoDesafioRequest("Se a base de um cilindro tem raio r e altura h, seu volume e:",
                                List.of("2 * pi * r * h", "pi * r^2 * h", "pi * r * h^2", "4/3 * pi * r^3"), 1, 30))));

        Missao missao3 = missaoService.criar(ana.getId(), curso.getId(), new NovaMissaoRequest(
                "Missao 3 - Logica Matematica e Algoritmos",
                "Desvende sequencias numericas, tabelas-verdade e raciocinio computacional",
                List.of(
                        new NovoDesafioRequest("Qual e o proximo numero na sequencia de Fibonacci: 1, 1, 2, 3, 5, 8, ... ?",
                                List.of("11", "13", "15", "21"), 1, 25),
                        new NovoDesafioRequest("Se a conjuncao (P E Q) e verdadeira, entao:",
                                List.of("P e falsa", "Q e falsa", "Ambas P e Q sao verdadeiras", "P ou Q e falsa"), 2, 30),
                        new NovoDesafioRequest("Qual operacao logica inverte o valor de verdade de uma proposicao?",
                                List.of("Porta AND", "Porta OR", "Porta NOT", "Porta XOR"), 2, 35))));

        // 4. Progresso Gamificado e Ranking
        // Carlos (Aluno #3): Conclui Missao 1 inteira (60 XP) + Desafio 1 e 2 da Missao 2 (45 XP) + Desafio 1 da Missao 3 (25 XP) = 130 XP
        // Ganha medalhas de 50 XP (Iniciante) e 100 XP (Aprendiz)!
        jogoService.responder(carlos.getId(), missao1.getDesafios().get(0).getId(), 1);
        jogoService.responder(carlos.getId(), missao1.getDesafios().get(1).getId(), 0);
        jogoService.responder(carlos.getId(), missao1.getDesafios().get(2).getId(), 1);
        jogoService.responder(carlos.getId(), missao2.getDesafios().get(0).getId(), 0);
        jogoService.responder(carlos.getId(), missao2.getDesafios().get(1).getId(), 1);
        jogoService.responder(carlos.getId(), missao3.getDesafios().get(0).getId(), 1);

        // Daniela (Aluna #4): Conclui Missao 1 desafios 1 e 2 (35 XP) + Missao 2 desafio 1 (20 XP) = 55 XP
        // Ganha medalha de 50 XP (Iniciante)! Tem uma tentativa incorreta registrada na Missao 1 desafio 3.
        jogoService.responder(daniela.getId(), missao1.getDesafios().get(0).getId(), 1);
        jogoService.responder(daniela.getId(), missao1.getDesafios().get(1).getId(), 0);
        jogoService.responder(daniela.getId(), missao1.getDesafios().get(2).getId(), 0); // Errou (tentativa gravada)
        jogoService.responder(daniela.getId(), missao2.getDesafios().get(0).getId(), 0);

        // Bruno (Aluno #2): Respondeu o primeiro desafio (15 XP). Pronto para jogar durante a apresentacao!
        jogoService.responder(bruno.getId(), missao1.getDesafios().get(0).getId(), 1);

        // 5. Notas Academicas para o Painel e Minhas Notas
        notaService.lancar(ana.getId(), curso.getId(), bruno.getId(), "Avaliacao Diagnostica", new BigDecimal("8.0"));
        notaService.lancar(ana.getId(), curso.getId(), bruno.getId(), "Trabalho em Grupo - Fracoes", new BigDecimal("7.5"));
        notaService.lancar(ana.getId(), curso.getId(), bruno.getId(), "Simulado Bimestral", new BigDecimal("8.5"));

        notaService.lancar(ana.getId(), curso.getId(), carlos.getId(), "Avaliacao Diagnostica", new BigDecimal("9.5"));
        notaService.lancar(ana.getId(), curso.getId(), carlos.getId(), "Trabalho em Grupo - Fracoes", new BigDecimal("10.0"));
        notaService.lancar(ana.getId(), curso.getId(), carlos.getId(), "Simulado Bimestral", new BigDecimal("9.0"));

        notaService.lancar(ana.getId(), curso.getId(), daniela.getId(), "Avaliacao Diagnostica", new BigDecimal("6.5"));
        notaService.lancar(ana.getId(), curso.getId(), daniela.getId(), "Trabalho em Grupo - Fracoes", new BigDecimal("7.0"));
        notaService.lancar(ana.getId(), curso.getId(), daniela.getId(), "Simulado Bimestral", new BigDecimal("6.0"));

        notaService.lancar(ana.getId(), curso.getId(), eduardo.getId(), "Avaliacao Diagnostica", new BigDecimal("5.0"));
        notaService.lancar(ana.getId(), curso.getId(), eduardo.getId(), "Trabalho em Grupo - Fracoes", new BigDecimal("6.0"));
        notaService.lancar(ana.getId(), curso.getId(), eduardo.getId(), "Simulado Bimestral", new BigDecimal("4.5"));

        // 6. Topicos e Respostas do Forum
        Post post1 = forumService.publicar(bruno.getId(), curso.getId(),
                "Duvida sobre soma de fracoes com denominadores diferentes",
                "Pessoal, na Missao 1 fiquei em duvida sobre como encontrar o MMC mais rapidamente. Alguem tem uma dica?");
        forumService.responder(ana.getId(), post1.getId(),
                "Ola Bruno! Uma tecnica pratica e fatorar os denominadores simultaneamente ou, quando forem primos entre si, basta multiplica-los!");
        forumService.responder(carlos.getId(), post1.getId(),
                "Eu costumo multiplicar cruzado para operacoes rapidas de comparacao, ajuda bastante!");

        Post post2 = forumService.publicar(carlos.getId(), curso.getId(),
                "Dicas para a Missao de Geometria Espacial",
                "Turma, para resolver o calculo de prismas e cilindros, desenhar a figura no papel antes ajuda muito a visualizar as bases!");
        forumService.responder(daniela.getId(), post2.getId(),
                "Boa dica Carlos! Eu estava esquecendo de elevar o raio ao quadrado na area do circulo.");
        forumService.responder(ana.getId(), post2.getId(),
                "Excelente colaboracao Carlos! A visualizacao espacial e fundamental na geometria.");

        Post post3 = forumService.publicar(daniela.getId(), curso.getId(),
                "Indicacoes de livros e exercicios de logica",
                "Alguem tem indicacoes de materiais complementares sobre proposicoes e portas logicas?");
        forumService.responder(bruno.getId(), post3.getId(),
                "Tem os modulos de matematica discreta na biblioteca e o assistente de IA aqui da plataforma tambem explica super bem!");

        // 7. Eventos de Telemetria IoT nos Laboratorios
        Instant agora = Instant.now();
        registroDeEventosIot.registrar(new EventoDeLaboratorio("evt-iot-001", "lab-1", "catraca-lab1",
                TipoEventoIot.PRESENCA, bruno.getId(), "Acesso ao Laboratorio Maker", agora.minus(120, ChronoUnit.MINUTES)));
        registroDeEventosIot.registrar(new EventoDeLaboratorio("evt-iot-002", "lab-1", "bancada-esp32-01",
                TipoEventoIot.INICIO_ATIVIDADE, bruno.getId(), "Montagem de Circuito com Sensor Ultrassonico", agora.minus(110, ChronoUnit.MINUTES)));
        registroDeEventosIot.registrar(new EventoDeLaboratorio("evt-iot-003", "lab-1", "catraca-lab1",
                TipoEventoIot.PRESENCA, carlos.getId(), "Acesso ao Laboratorio Maker", agora.minus(90, ChronoUnit.MINUTES)));
        registroDeEventosIot.registrar(new EventoDeLaboratorio("evt-iot-004", "lab-1", "bancada-esp32-02",
                TipoEventoIot.INICIO_ATIVIDADE, carlos.getId(), "Calibracao de Sensor de Luminosidade LDR", agora.minus(80, ChronoUnit.MINUTES)));
        registroDeEventosIot.registrar(new EventoDeLaboratorio("evt-iot-005", "lab-1", "bancada-esp32-01",
                TipoEventoIot.CONCLUSAO_ATIVIDADE, bruno.getId(), "Montagem de Circuito com Sensor Ultrassonico", agora.minus(40, ChronoUnit.MINUTES)));
        registroDeEventosIot.registrar(new EventoDeLaboratorio("evt-iot-006", "lab-1", "bancada-esp32-03",
                TipoEventoIot.INICIO_ATIVIDADE, daniela.getId(), "Gravacao de Firmware no Microcontrolador", agora.minus(30, ChronoUnit.MINUTES)));

        registroDeEventosIot.registrar(new EventoDeLaboratorio("evt-iot-007", "fisica", "sensor-optico-01",
                TipoEventoIot.INICIO_ATIVIDADE, carlos.getId(), "Medicao de Velocidade Instantanea no Trilho de Ar", agora.minus(180, ChronoUnit.MINUTES)));
        registroDeEventosIot.registrar(new EventoDeLaboratorio("evt-iot-008", "fisica", "sensor-optico-01",
                TipoEventoIot.CONCLUSAO_ATIVIDADE, carlos.getId(), "Medicao de Velocidade Instantanea no Trilho de Ar", agora.minus(140, ChronoUnit.MINUTES)));

        registroDeEventosIot.registrar(new EventoDeLaboratorio("evt-iot-009", "quimica", "sensor-ph-01",
                TipoEventoIot.INICIO_ATIVIDADE, daniela.getId(), "Titulacao e Analise Potenciometrica de pH", agora.minus(200, ChronoUnit.MINUTES)));
        registroDeEventosIot.registrar(new EventoDeLaboratorio("evt-iot-010", "quimica", "sensor-ph-01",
                TipoEventoIot.CONCLUSAO_ATIVIDADE, daniela.getId(), "Titulacao e Analise Potenciometrica de pH", agora.minus(150, ChronoUnit.MINUTES)));

        log.info("""

                ==================== DADOS DE APRESENTACAO (profile dev) ====================
                 Professor   Professora Ana  id={}  (responsavel pelo curso)
                 Aluno       Bruno           id={}  (matriculado, 15 XP, rank 3)
                 Aluno       Carlos          id={}  (matriculado, 130 XP, rank 1, 2 medalhas)
                 Aluno       Daniela         id={}  (matriculado, 55 XP, rank 2, 1 medalha)
                 Aluno       Eduardo         id={}  (matriculado, 0 XP, rank 4)
                 Curso       id={}  "{}"
                 Missoes     3 missoes criadas (9 desafios no total)
                 Notas       Lancadas para todos os alunos (medias, aprovados e recuperacao)
                 Forum       3 topicos ativos com interacoes e respostas
                 IoT         10 eventos registrados em 3 laboratorios (lab-1, fisica, quimica)
                =============================================================================
                """,
                ana.getId(), bruno.getId(), carlos.getId(), daniela.getId(), eduardo.getId(),
                curso.getId(), curso.getTitulo());
    }
}
