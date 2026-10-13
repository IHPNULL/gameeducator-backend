package org.facens.grupo_6_gameeducator.gamificacao;

import java.util.concurrent.atomic.AtomicInteger;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Papel;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.gamificacao.porta.RepositorioDeMedalhas;
import org.facens.grupo_6_gameeducator.gamificacao.porta.RepositorioDeProgresso;
import org.facens.grupo_6_gameeducator.repository.CursoRepository;
import org.facens.grupo_6_gameeducator.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@DisplayName("Contrato das portas de gamificacao - adaptador JPA")
class RepositoriosJpaTest extends ContratoRepositoriosDeGamificacao {

    private static final AtomicInteger SEQUENCIA = new AtomicInteger();

    @Autowired
    private RepositorioDeProgresso progressos;
    @Autowired
    private RepositorioDeMedalhas medalhas;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CursoRepository cursoRepository;

    @Override
    protected RepositorioDeProgresso progressos() {
        return progressos;
    }

    @Override
    protected RepositorioDeMedalhas medalhas() {
        return medalhas;
    }

    @Override
    protected Long novoAluno(String nome) {
        return usuarioRepository.save(new Usuario(nome, "contrato" + SEQUENCIA.incrementAndGet() + "@facens.br", Papel.ALUNO)).getId();
    }

    @Override
    protected Long novoCurso() {
        Usuario professor = usuarioRepository.save(
                new Usuario("Ana", "prof" + SEQUENCIA.incrementAndGet() + "@facens.br", Papel.PROFESSOR));
        return cursoRepository.save(new Curso("Curso " + SEQUENCIA.get(), professor)).getId();
    }
}
