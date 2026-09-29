package org.facens.grupo_6_gameeducator.repository;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Missao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MissaoRepository extends JpaRepository<Missao, Long> {

    /**
     * Carrega os desafios junto com a missao: a serializacao acontece fora da transacao
     * (open-in-view=false), entao o lazy loading falharia na camada web.
     */
    @Query("""
            select distinct m from Missao m
            left join fetch m.desafios
            where m.curso.id = :cursoId
            order by m.id asc
            """)
    List<Missao> findByCursoIdOrderByIdAsc(Long cursoId);
}
