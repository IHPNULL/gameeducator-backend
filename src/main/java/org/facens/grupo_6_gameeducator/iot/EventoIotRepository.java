package org.facens.grupo_6_gameeducator.iot;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoIotRepository extends JpaRepository<EventoIot, String> {

    List<EventoIot> findTop100ByLaboratorioOrderByOcorridoEmDesc(String laboratorio);

    long countByLaboratorio(String laboratorio);
}
