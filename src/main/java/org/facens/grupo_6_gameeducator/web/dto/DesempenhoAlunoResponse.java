package org.facens.grupo_6_gameeducator.web.dto;

import org.facens.grupo_6_gameeducator.service.dto.DesempenhoAluno;

public record DesempenhoAlunoResponse(
        Long alunoId,
        String nome,
        int xpTotal,
        int tentativas,
        int acertos,
        double taxaAcerto
) {

    public static DesempenhoAlunoResponse de(DesempenhoAluno desempenho) {
        double taxaAcerto = desempenho.tentativas() == 0
                ? 0.0
                : (double) desempenho.acertos() / desempenho.tentativas();
        return new DesempenhoAlunoResponse(
                desempenho.aluno().getId(),
                desempenho.aluno().getNome(),
                desempenho.xpTotal(),
                desempenho.tentativas(),
                desempenho.acertos(),
                taxaAcerto);
    }
}
