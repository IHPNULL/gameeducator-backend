package org.facens.grupo_6_gameeducator.repository;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Medalha;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedalhaRepository extends JpaRepository<Medalha, Long> {

    boolean existsByAlunoIdAndCursoIdAndMarcoXp(Long alunoId, Long cursoId, int marcoXp);

    List<Medalha> findByAlunoIdAndCursoIdOrderByMarcoXpAsc(Long alunoId, Long cursoId);
}
