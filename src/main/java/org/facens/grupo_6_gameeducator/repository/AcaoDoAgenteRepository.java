package org.facens.grupo_6_gameeducator.repository;

import java.util.List;
import java.util.Optional;
import org.facens.grupo_6_gameeducator.domain.AcaoDoAgente;
import org.facens.grupo_6_gameeducator.domain.StatusAcaoDoAgente;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AcaoDoAgenteRepository extends JpaRepository<AcaoDoAgente, Long> {

    List<AcaoDoAgente> findBySolicitanteIdAndCursoIdOrderByCriadaEmDescIdDesc(Long solicitanteId, Long cursoId);

    List<AcaoDoAgente> findBySolicitanteIdAndCursoIdAndStatusOrderByCriadaEmDescIdDesc(Long solicitanteId, Long cursoId,
                                                                                        StatusAcaoDoAgente status);

    /** Trava a linha ate o fim da transacao: duas aprovacoes simultaneas da mesma acao viram uma fila. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AcaoDoAgente a where a.id = :id")
    Optional<AcaoDoAgente> travarParaDecidir(@Param("id") Long id);
}
