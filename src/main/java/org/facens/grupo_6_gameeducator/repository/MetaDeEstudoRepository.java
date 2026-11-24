package org.facens.grupo_6_gameeducator.repository;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.MetaDeEstudo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetaDeEstudoRepository extends JpaRepository<MetaDeEstudo, Long> {

    List<MetaDeEstudo> findByAlunoIdAndCursoIdOrderByPrazoAscIdAsc(Long alunoId, Long cursoId);
}
