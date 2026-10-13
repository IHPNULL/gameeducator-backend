package org.facens.grupo_6_gameeducator.ia;

import java.util.List;

/** Pergunta do aluno + material do curso (missoes e enunciados) usado como base do RAG. */
public record PerguntaAoAssistente(Long cursoId, String pergunta, List<Documento> documentos) {

    public record Documento(String id, String titulo, String texto) {
    }
}
