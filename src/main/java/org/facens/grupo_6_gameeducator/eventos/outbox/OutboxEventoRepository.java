package org.facens.grupo_6_gameeducator.eventos.outbox;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventoRepository extends JpaRepository<OutboxEvento, String> {

    List<OutboxEvento> findTop50ByPublicadoEmIsNullOrderByCriadoEmAsc();

    long countByPublicadoEmIsNull();
}
