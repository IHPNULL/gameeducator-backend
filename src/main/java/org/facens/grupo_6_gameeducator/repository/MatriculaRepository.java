package org.facens.grupo_6_gameeducator.repository;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Matricula;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    boolean existsByCursoIdAndAlunoId(Long cursoId, Long alunoId);

    List<Matricula> findByAlunoId(Long alunoId);

    List<Matricula> findByCursoId(Long cursoId);
}
