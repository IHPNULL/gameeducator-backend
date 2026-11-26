package org.facens.grupo_6_gameeducator.web.dto;

import java.util.List;
import org.facens.grupo_6_gameeducator.service.agente.AgenteService.RespostaDoAgente;

/** agente = "orientacao" (aluno) | "acompanhamento" (professor); acoesPendentes aguardam aprovacao. */
public record RespostaDoAgenteResponse(String agente, String resposta, List<AcaoDoAgenteResponse> acoesPendentes) {

    public static RespostaDoAgenteResponse de(RespostaDoAgente resposta) {
        return new RespostaDoAgenteResponse(resposta.agente(), resposta.resposta(),
                resposta.acoesPendentes().stream().map(AcaoDoAgenteResponse::de).toList());
    }
}
