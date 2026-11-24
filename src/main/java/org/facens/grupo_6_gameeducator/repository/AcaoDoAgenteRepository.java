package org.facens.grupo_6_gameeducator.repository;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.AcaoDoAgente;
import org.facens.grupo_6_gameeducator.domain.StatusAcaoDoAgente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcaoDoAgenteRepository extends JpaRepository<AcaoDoAgente, Long> {

    List<AcaoDoAgente> findBySolicitanteIdAndCursoIdOrderByCriadaEmDescIdDesc(Long solicitanteId, Long cursoId);

    List<AcaoDoAgente> findBySolicitanteIdAndCursoIdAndStatusOrderByCriadaEmDescIdDesc(Long solicitanteId, Long cursoId,
                                                                                        StatusAcaoDoAgente status);
}
