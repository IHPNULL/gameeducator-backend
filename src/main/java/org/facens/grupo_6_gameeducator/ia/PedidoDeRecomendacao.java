package org.facens.grupo_6_gameeducator.ia;

import java.util.List;

/** Contexto que o Core envia a IA (o servico de IA e stateless e nao le o banco do Core). */
public record PedidoDeRecomendacao(Long alunoId, Long cursoId, List<MissaoParaRecomendar> missoes, int limite) {

    public record MissaoParaRecomendar(Long missaoId, String titulo, int totalDesafios, int desafiosAcertados,
                                       int tentativas, int xpTotal) {

        public boolean concluida() {
            return totalDesafios > 0 && desafiosAcertados >= totalDesafios;
        }
    }
}
