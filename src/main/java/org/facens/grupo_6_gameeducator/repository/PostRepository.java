package org.facens.grupo_6_gameeducator.repository;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    /** Posts do forum do curso, do mais recente para o mais antigo. */
    List<Post> findByCursoIdOrderByDataHoraDescIdDesc(Long cursoId);
}
