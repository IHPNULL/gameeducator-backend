package org.facens.grupo_6_gameeducator.repository;

import java.util.List;
import java.util.Optional;
import org.facens.grupo_6_gameeducator.domain.Nota;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotaRepository extends JpaRepository<Nota, Long> {

    /** Notas do aluno naquele curso: o que a aba de notas exibe. */
    List<Nota> findByAlunoIdAndCursoIdOrderByIdAsc(Long alunoId, Long cursoId);

    Optional<Nota> findByAlunoIdAndCursoIdAndAvaliacao(Long alunoId, Long cursoId, String avaliacao);
}
