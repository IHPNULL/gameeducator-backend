package org.facens.grupo_6_gameeducator.repository;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.RespostaPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RespostaPostRepository extends JpaRepository<RespostaPost, Long> {

    List<RespostaPost> findByPostIdOrderByIdAsc(Long postId);
}
