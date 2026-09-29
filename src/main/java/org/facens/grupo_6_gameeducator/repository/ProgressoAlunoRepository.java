package org.facens.grupo_6_gameeducator.repository;

import java.util.List;
import java.util.Optional;
import org.facens.grupo_6_gameeducator.domain.ProgressoAluno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProgressoAlunoRepository extends JpaRepository<ProgressoAluno, Long> {

    Optional<ProgressoAluno> findByAlunoIdAndCursoId(Long alunoId, Long cursoId);

    /**
     * Base do ranking da turma. O aluno vem no mesmo select porque o nome dele
     * e lido durante a serializacao, ja fora da transacao.
     */
    @Query("""
            select p from ProgressoAluno p
            join fetch p.aluno
            where p.curso.id = :cursoId
            order by p.xpTotal desc
            """)
    List<ProgressoAluno> findByCursoIdOrderByXpTotalDesc(Long cursoId);
}
