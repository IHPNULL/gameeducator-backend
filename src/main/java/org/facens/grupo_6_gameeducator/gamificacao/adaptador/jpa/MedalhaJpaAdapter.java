package org.facens.grupo_6_gameeducator.gamificacao.adaptador.jpa;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.gamificacao.MedalhaDoAluno;
import org.facens.grupo_6_gameeducator.gamificacao.porta.RepositorioDeMedalhas;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador JPA da porta de medalhas (tabela medalha). */
@Transactional(readOnly = true)
public class MedalhaJpaAdapter implements RepositorioDeMedalhas {

    private final MedalhaRepository repository;
    private final EntityManager entityManager;

    public MedalhaJpaAdapter(MedalhaRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public Set<Integer> marcosConquistados(Long alunoId, Long cursoId) {
        return repository.findByAlunoIdAndCursoIdOrderByMarcoXpAsc(alunoId, cursoId).stream()
                .map(Medalha::getMarcoXp)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public void registrar(Long alunoId, Long cursoId, int marcoXp) {
        repository.save(new Medalha(
                entityManager.getReference(Usuario.class, alunoId),
                entityManager.getReference(Curso.class, cursoId),
                marcoXp));
    }

    @Override
    public List<MedalhaDoAluno> listar(Long alunoId, Long cursoId) {
        return repository.findByAlunoIdAndCursoIdOrderByMarcoXpAsc(alunoId, cursoId).stream()
                .map(medalha -> new MedalhaDoAluno(medalha.getMarcoXp(), medalha.getDataConcedida()))
                .toList();
    }
}
