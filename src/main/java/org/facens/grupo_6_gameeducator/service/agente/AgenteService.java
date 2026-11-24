package org.facens.grupo_6_gameeducator.service.agente;

import java.time.LocalDate;
import java.util.List;
import org.facens.grupo_6_gameeducator.domain.AcaoDoAgente;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.exception.AcessoNegadoException;
import org.facens.grupo_6_gameeducator.exception.RecursoNaoEncontradoException;
import org.facens.grupo_6_gameeducator.exception.ServicoIndisponivelException;
import org.facens.grupo_6_gameeducator.gamificacao.GamificacaoApi;
import org.facens.grupo_6_gameeducator.ia.IaIndisponivelException;
import org.facens.grupo_6_gameeducator.ia.OrquestradorDeAgente;
import org.facens.grupo_6_gameeducator.ia.OrquestradorDeAgente.PedidoAoAgente;
import org.facens.grupo_6_gameeducator.repository.CursoRepository;
import org.facens.grupo_6_gameeducator.repository.MatriculaRepository;
import org.facens.grupo_6_gameeducator.repository.UsuarioRepository;
import org.facens.grupo_6_gameeducator.service.AssistenteService;
import org.facens.grupo_6_gameeducator.service.JogoService;
import org.facens.grupo_6_gameeducator.service.RecomendacaoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * RF04 - agentes de IA que coordenam orientacao (aluno) e acompanhamento (professor).
 *
 * <p>O papel do usuario escolhe o agente e, com ele, o conjunto de ferramentas: o aluno nunca recebe as
 * ferramentas da turma e o professor so as recebe para o curso que e dele (RNF03). A chamada ao LLM roda
 * FORA de transacao (pode levar segundos). Sem LLM, o agente responde 503 e o resto da plataforma segue.
 */
@Service
public class AgenteService {

    public static final String ORIENTACAO = "orientacao";
    public static final String ACOMPANHAMENTO = "acompanhamento";
    static final String MENSAGEM_INDISPONIVEL =
            "O agente de IA esta indisponivel no momento (o modelo pode estar carregando). Tente de novo em instantes; "
                    + "o restante da plataforma segue funcionando.";

    private final UsuarioRepository usuarios;
    private final CursoRepository cursos;
    private final MatriculaRepository matriculas;
    private final OrquestradorDeAgente orquestrador;
    private final AcoesDoAgenteService acoes;
    private final GamificacaoApi gamificacao;
    private final RecomendacaoService recomendacoes;
    private final AssistenteService assistente;
    private final JogoService jogo;
    private final TransactionTemplate leitura;

    public AgenteService(UsuarioRepository usuarios, CursoRepository cursos, MatriculaRepository matriculas,
                         OrquestradorDeAgente orquestrador, AcoesDoAgenteService acoes, GamificacaoApi gamificacao,
                         RecomendacaoService recomendacoes, AssistenteService assistente, JogoService jogo,
                         PlatformTransactionManager transactionManager) {
        this.usuarios = usuarios;
        this.cursos = cursos;
        this.matriculas = matriculas;
        this.orquestrador = orquestrador;
        this.acoes = acoes;
        this.gamificacao = gamificacao;
        this.recomendacoes = recomendacoes;
        this.assistente = assistente;
        this.jogo = jogo;
        this.leitura = new TransactionTemplate(transactionManager);
        this.leitura.setReadOnly(true);
    }

    public record RespostaDoAgente(String agente, String resposta, List<AcaoDoAgente> acoesPendentes) {
    }

    private record Participante(boolean aluno, String nome, String curso) {
    }

    public RespostaDoAgente conversar(Long usuarioId, Long cursoId, String mensagem) {
        Participante quem = leitura.execute(status -> identificar(usuarioId, cursoId));
        String agente = quem.aluno() ? ORIENTACAO : ACOMPANHAMENTO;
        PedidoAoAgente pedido = new PedidoAoAgente("usuario-" + usuarioId + "-curso-" + cursoId,
                instrucoes(quem), mensagem, List.of(ferramentas(quem, usuarioId, cursoId)));
        String resposta;
        try {
            resposta = orquestrador.conduzir(pedido);
        } catch (IaIndisponivelException e) {
            throw new ServicoIndisponivelException(MENSAGEM_INDISPONIVEL);
        }
        return new RespostaDoAgente(agente, resposta, acoes.pendentes(usuarioId, cursoId));
    }

    /** Quem pode conversar: aluno matriculado no curso ou o professor responsavel por ele. */
    private Participante identificar(Long usuarioId, Long cursoId) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Usuario", usuarioId));
        Curso curso = cursos.findById(cursoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Curso", cursoId));
        boolean permitido = usuario.isAluno()
                ? matriculas.existsByCursoIdAndAlunoId(cursoId, usuarioId)
                : curso.ehResponsavel(usuario);
        if (!permitido) {
            throw new AcessoNegadoException("Usuario " + usuarioId + " nao participa do curso " + cursoId);
        }
        return new Participante(usuario.isAluno(), usuario.getNome(), curso.getTitulo());
    }

    private Object ferramentas(Participante quem, Long usuarioId, Long cursoId) {
        return quem.aluno()
                ? new FerramentasDeOrientacao(usuarioId, cursoId, gamificacao, recomendacoes, assistente, acoes)
                : new FerramentasDeAcompanhamento(usuarioId, cursoId, jogo, gamificacao, acoes);
    }

    static String instrucoes(Participante quem) {
        String comum = """
                Hoje e %s. Voce conversa com %s no curso "%s" da plataforma GameEducator.
                Regras:
                - Responda sempre em portugues do Brasil, de forma curta e pratica.
                - Use as ferramentas para obter dados; nunca invente numeros, missoes ou alunos.
                - Voce NAO executa acoes sozinho. Quando o usuario pedir para criar, propor, sugerir ou enviar algo, \
                voce DEVE chamar a ferramenta %s. Sem essa chamada nao existe proposta: nunca diga que algo esta \
                pendente se a ferramenta nao devolveu "Proposta #N". Depois, diga que a proposta #N esta AGUARDANDO \
                APROVACAO no painel de acoes. Nunca diga que algo foi aprovado, criado ou enviado: so o usuario aprova.
                - Voce so tem acesso aos dados que suas ferramentas devolvem. Se pedirem algo fora delas, diga \
                que voce nao tem permissao para isso.
                - Se uma ferramenta recusar, explique o motivo ao usuario.
                """.formatted(LocalDate.now(), quem.nome(), quem.curso(),
                quem.aluno() ? "proporMetaDeEstudo" : "proporLembreteAoAluno");
        String papel = quem.aluno()
                ? """
                Voce e o AGENTE DE ORIENTACAO do aluno. Ajude-o a decidir o que estudar: consulte o progresso, \
                recomende missoes, tire duvidas com o material do curso e, quando fizer sentido, proponha uma meta \
                de estudo com prazo."""
                : """
                Voce e o AGENTE DE ACOMPANHAMENTO do professor. Analise o desempenho e o ranking da turma, aponte \
                alunos com dificuldade (muitas tentativas e poucos acertos, ou pouco XP) e, quando o professor quiser, \
                proponha lembretes para esses alunos. O texto do lembrete fala diretamente com o aluno, com respeito \
                e incentivo, e cita a missao ou o ponto a melhorar.""";
        return comum + papel;
    }
}
