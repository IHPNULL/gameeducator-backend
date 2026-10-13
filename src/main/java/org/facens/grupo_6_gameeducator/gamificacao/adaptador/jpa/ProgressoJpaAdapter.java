package org.facens.grupo_6_gameeducator.gamificacao.adaptador.jpa;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.stream.IntStream;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.gamificacao.PosicaoNoRanking;
import org.facens.grupo_6_gameeducator.gamificacao.dominio.Progresso;
import org.facens.grupo_6_gameeducator.gamificacao.porta.RepositorioDeProgresso;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador JPA da porta de progresso: traduz {@link Progresso} de/para a tabela progresso_aluno. */
@Transactional(readOnly = true)
public class ProgressoJpaAdapter implements RepositorioDeProgresso {

    private final ProgressoAlunoRepository repository;
    private final EntityManager entityManager;

    public ProgressoJpaAdapter(ProgressoAlunoRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public Progresso carregar(Long alunoId, Long cursoId) {
        return repository.findByAlunoIdAndCursoId(alunoId, cursoId)
                .map(entidade -> new Progresso(alunoId, cursoId, entidade.getXpTotal()))
                .orElseGet(() -> Progresso.inicial(alunoId, cursoId));
    }

    @Override
    @Transactional
    public void salvar(Progresso progresso) {
        ProgressoAluno entidade = repository.findByAlunoIdAndCursoId(progresso.alunoId(), progresso.cursoId())
                .orElseGet(() -> new ProgressoAluno(
                        entityManager.getReference(Usuario.class, progresso.alunoId()),
                        entityManager.getReference(Curso.class, progresso.cursoId())));
        entidade.setXpTotal(progresso.xpTotal());
        repository.save(entidade);
    }

    @Override
    public List<PosicaoNoRanking> ranking(Long cursoId) {
        List<ProgressoAluno> linhas = repository.findByCursoIdOrderByXpTotalDesc(cursoId);
        return IntStream.range(0, linhas.size())
                .mapToObj(i -> new PosicaoNoRanking(
                        i + 1,
                        linhas.get(i).getAluno().getId(),
                        linhas.get(i).getAluno().getNome(),
                        linhas.get(i).getXpTotal()))
                .toList();
    }
}
