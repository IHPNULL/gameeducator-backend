package org.facens.grupo_6_gameeducator.web.dto;

import java.time.LocalDateTime;
import org.facens.grupo_6_gameeducator.domain.Medalha;

public record MedalhaResponse(int marcoXp, LocalDateTime dataConcedida) {

    public static MedalhaResponse de(Medalha medalha) {
        return new MedalhaResponse(medalha.getMarcoXp(), medalha.getDataConcedida());
    }
}
