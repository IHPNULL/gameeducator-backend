package org.facens.grupo_6_gameeducator.gamificacao;

import java.util.concurrent.atomic.AtomicLong;
import org.facens.grupo_6_gameeducator.gamificacao.adaptador.memoria.GamificacaoEmMemoria;
import org.facens.grupo_6_gameeducator.gamificacao.porta.RepositorioDeMedalhas;
import org.facens.grupo_6_gameeducator.gamificacao.porta.RepositorioDeProgresso;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Contrato das portas de gamificacao - adaptador EM MEMORIA")
class RepositoriosEmMemoriaTest extends ContratoRepositoriosDeGamificacao {

    private final GamificacaoEmMemoria memoria = new GamificacaoEmMemoria();
    private final AtomicLong ids = new AtomicLong();

    @Override
    protected RepositorioDeProgresso progressos() {
        return memoria;
    }

    @Override
    protected RepositorioDeMedalhas medalhas() {
        return memoria;
    }

    @Override
    protected Long novoAluno(String nome) {
        long id = ids.incrementAndGet();
        memoria.registrarNome(id, nome);
        return id;
    }

    @Override
    protected Long novoCurso() {
        return ids.incrementAndGet();
    }
}
