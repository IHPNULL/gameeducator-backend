package org.facens.grupo_6_gameeducator.ia.adaptador;

import org.facens.grupo_6_gameeducator.ia.IaIndisponivelException;
import org.facens.grupo_6_gameeducator.ia.OrquestradorDeAgente;

/** app.agente.habilitado=false (padrao em dev e nos testes): sem LLM, o agente responde como indisponivel. */
public class AgenteDesabilitado implements OrquestradorDeAgente {

    @Override
    public String conduzir(PedidoAoAgente pedido) {
        throw new IaIndisponivelException("Agente de IA desabilitado (app.agente.habilitado=false)", null);
    }
}
