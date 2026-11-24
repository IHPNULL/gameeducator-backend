package org.facens.grupo_6_gameeducator.service.agente;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.facens.grupo_6_gameeducator.domain.AcaoDoAgente;
import org.facens.grupo_6_gameeducator.domain.MetaDeEstudo;
import org.facens.grupo_6_gameeducator.domain.Missao;
import org.facens.grupo_6_gameeducator.domain.StatusAcaoDoAgente;
import org.facens.grupo_6_gameeducator.domain.TipoAcaoDoAgente;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.eventos.EmissorDeEventos;
import org.facens.grupo_6_gameeducator.eventos.LembreteAoAlunoEnviado;
import org.facens.grupo_6_gameeducator.exception.AcessoNegadoException;
import org.facens.grupo_6_gameeducator.exception.RecursoNaoEncontradoException;
import org.facens.grupo_6_gameeducator.exception.RegraDeNegocioException;
import org.facens.grupo_6_gameeducator.repository.AcaoDoAgenteRepository;
import org.facens.grupo_6_gameeducator.repository.MatriculaRepository;
import org.facens.grupo_6_gameeducator.repository.MetaDeEstudoRepository;
import org.facens.grupo_6_gameeducator.repository.MissaoRepository;
import org.facens.grupo_6_gameeducator.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF04 - "com autorizacao do usuario": as ferramentas de acao dos agentes so PROPOEM; esta classe guarda a
 * proposta como PENDENTE e so a executa quando o proprio usuario aprova.
 *
 * <p>Regras: so o dono da conversa decide; cada proposta e decidida uma vez; as regras de negocio (missao do
 * curso, aluno matriculado) sao checadas ao propor E de novo ao executar.
 */
@Service
public class AcoesDoAgenteService {

    static final int PRAZO_MAXIMO_DIAS = 90;

    private final AcaoDoAgenteRepository acoes;
    private final MetaDeEstudoRepository metas;
    private final MissaoRepository missoes;
    private final MatriculaRepository matriculas;
    private final UsuarioRepository usuarios;
    private final EmissorDeEventos eventos;
    private final Clock relogio;

    @Autowired
    public AcoesDoAgenteService(AcaoDoAgenteRepository acoes, MetaDeEstudoRepository metas, MissaoRepository missoes,
                                MatriculaRepository matriculas, UsuarioRepository usuarios, EmissorDeEventos eventos) {
        this(acoes, metas, missoes, matriculas, usuarios, eventos, Clock.systemDefaultZone());
    }

    AcoesDoAgenteService(AcaoDoAgenteRepository acoes, MetaDeEstudoRepository metas, MissaoRepository missoes,
                         MatriculaRepository matriculas, UsuarioRepository usuarios, EmissorDeEventos eventos,
                         Clock relogio) {
        this.acoes = acoes;
        this.metas = metas;
        this.missoes = missoes;
        this.matriculas = matriculas;
        this.usuarios = usuarios;
        this.eventos = eventos;
        this.relogio = relogio;
    }

    /** Ferramenta do agente de orientacao: propoe ao aluno uma meta para concluir uma missao do curso. */
    @Transactional
    public AcaoDoAgente proporMetaDeEstudo(Long alunoId, Long cursoId, Long missaoId, int prazoEmDias, String objetivo) {
        Missao missao = missoes.findById(missaoId)
                .filter(m -> m.getCurso().getId().equals(cursoId))
                .orElseThrow(() -> new RegraDeNegocioException("A missao " + missaoId + " nao existe neste curso"));
        if (prazoEmDias < 1 || prazoEmDias > PRAZO_MAXIMO_DIAS) {
            throw new RegraDeNegocioException("O prazo deve ficar entre 1 e " + PRAZO_MAXIMO_DIAS + " dias");
        }
        LocalDate prazo = LocalDate.now(relogio).plusDays(prazoEmDias);
        String texto = objetivo == null ? "" : objetivo.strip();
        return acoes.save(AcaoDoAgente.metaDeEstudo(alunoId, cursoId, missao.getId(), missao.getTitulo(), prazo, texto,
                agora()));
    }

    /** Ferramenta do agente de acompanhamento: propoe ao professor um lembrete para um aluno da turma. */
    @Transactional
    public AcaoDoAgente proporLembrete(Long professorId, Long cursoId, Long alunoId, String mensagem) {
        if (mensagem == null || mensagem.isBlank()) {
            throw new RegraDeNegocioException("O lembrete precisa de uma mensagem");
        }
        exigirMatricula(cursoId, alunoId);
        Usuario aluno = usuarios.findById(alunoId).orElseThrow(() -> RecursoNaoEncontradoException.de("Usuario", alunoId));
        return acoes.save(AcaoDoAgente.lembreteAoAluno(professorId, cursoId, alunoId, aluno.getNome(), mensagem.strip(),
                agora()));
    }

    @Transactional(readOnly = true)
    public List<AcaoDoAgente> listar(Long usuarioId, Long cursoId) {
        return acoes.findBySolicitanteIdAndCursoIdOrderByCriadaEmDescIdDesc(usuarioId, cursoId);
    }

    @Transactional(readOnly = true)
    public List<AcaoDoAgente> pendentes(Long usuarioId, Long cursoId) {
        return acoes.findBySolicitanteIdAndCursoIdAndStatusOrderByCriadaEmDescIdDesc(usuarioId, cursoId,
                StatusAcaoDoAgente.PENDENTE);
    }

    /** O usuario autoriza: o Core executa a acao (nao o agente) e registra quando foi decidida. */
    @Transactional
    public AcaoDoAgente aprovar(Long usuarioId, Long cursoId, Long acaoId) {
        AcaoDoAgente acao = paraDecidir(usuarioId, cursoId, acaoId);
        Instant agora = agora();
        if (acao.getTipo() == TipoAcaoDoAgente.META_DE_ESTUDO) {
            metas.save(new MetaDeEstudo(acao, agora));
        } else {
            exigirMatricula(cursoId, acao.getAlunoAlvoId());
            eventos.emitir(new LembreteAoAlunoEnviado(UUID.randomUUID().toString(), acao.getAlunoAlvoId(), cursoId,
                    usuarioId, acao.getMensagem(), acao.getId(), agora));
        }
        acao.marcarExecutada(agora);
        return acao;
    }

    @Transactional
    public AcaoDoAgente rejeitar(Long usuarioId, Long cursoId, Long acaoId) {
        AcaoDoAgente acao = paraDecidir(usuarioId, cursoId, acaoId);
        acao.marcarRejeitada(agora());
        return acao;
    }

    @Transactional(readOnly = true)
    public List<MetaDeEstudo> metas(Long alunoId, Long cursoId) {
        return metas.findByAlunoIdAndCursoIdOrderByPrazoAscIdAsc(alunoId, cursoId);
    }

    private AcaoDoAgente paraDecidir(Long usuarioId, Long cursoId, Long acaoId) {
        AcaoDoAgente acao = acoes.findById(acaoId)
                .filter(a -> a.getCursoId().equals(cursoId))
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Acao do agente", acaoId));
        if (!acao.getSolicitanteId().equals(usuarioId)) {
            throw new AcessoNegadoException("Somente quem conversou com o agente pode decidir a acao " + acaoId);
        }
        if (!acao.pendente()) {
            throw new RegraDeNegocioException("A acao " + acaoId + " ja foi decidida (" + acao.getStatus() + ")");
        }
        return acao;
    }

    private void exigirMatricula(Long cursoId, Long alunoId) {
        if (!matriculas.existsByCursoIdAndAlunoId(cursoId, alunoId)) {
            throw new RegraDeNegocioException("O aluno " + alunoId + " nao esta matriculado neste curso");
        }
    }

    private Instant agora() {
        return Instant.now(relogio);
    }
}
