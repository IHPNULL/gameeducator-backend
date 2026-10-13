package org.facens.grupo_6_gameeducator.gamificacao.adaptador.jpa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedalhaRepository extends JpaRepository<Medalha, Long> {

    boolean existsByAlunoIdAndCursoIdAndMarcoXp(Long alunoId, Long cursoId, int marcoXp);

    List<Medalha> findByAlunoIdAndCursoIdOrderByMarcoXpAsc(Long alunoId, Long cursoId);
}
