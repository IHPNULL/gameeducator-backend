package org.facens.grupo_6_gameeducator.gamificacao.config;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.facens.grupo_6_gameeducator.gamificacao.GamificacaoApi;
import org.facens.grupo_6_gameeducator.gamificacao.adaptador.jpa.MedalhaJpaAdapter;
import org.facens.grupo_6_gameeducator.gamificacao.adaptador.jpa.MedalhaRepository;
import org.facens.grupo_6_gameeducator.gamificacao.adaptador.jpa.ProgressoAlunoRepository;
import org.facens.grupo_6_gameeducator.gamificacao.adaptador.jpa.ProgressoJpaAdapter;
import org.facens.grupo_6_gameeducator.gamificacao.adaptador.memoria.GamificacaoEmMemoria;
import org.facens.grupo_6_gameeducator.gamificacao.aplicacao.GamificacaoService;
import org.facens.grupo_6_gameeducator.gamificacao.dominio.PoliticaDeMedalhas;
import org.facens.grupo_6_gameeducator.gamificacao.porta.RepositorioDeMedalhas;
import org.facens.grupo_6_gameeducator.gamificacao.porta.RepositorioDeProgresso;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Montagem do modulo de gamificacao: o unico lugar que conhece as implementacoes concretas.
 * {@code app.gamificacao.persistencia} escolhe o adaptador (jpa = padrao, memoria = Desafio 2).
 */
@Configuration
public class GamificacaoConfig {

    @Bean
    PoliticaDeMedalhas politicaDeMedalhas(
            @Value("${app.gamificacao.marcos-xp:50,100,250,500}") List<Integer> marcosXp) {
        return new PoliticaDeMedalhas(marcosXp);
    }

    @Bean
    GamificacaoApi gamificacaoApi(RepositorioDeProgresso progressos,
                                  RepositorioDeMedalhas medalhas,
                                  PoliticaDeMedalhas politicaDeMedalhas) {
        return new GamificacaoService(progressos, medalhas, politicaDeMedalhas);
    }

    @Configuration
    @ConditionalOnProperty(name = "app.gamificacao.persistencia", havingValue = "jpa", matchIfMissing = true)
    static class PersistenciaJpa {

        @Bean
        RepositorioDeProgresso repositorioDeProgresso(ProgressoAlunoRepository repository, EntityManager entityManager) {
            return new ProgressoJpaAdapter(repository, entityManager);
        }

        @Bean
        RepositorioDeMedalhas repositorioDeMedalhas(MedalhaRepository repository, EntityManager entityManager) {
            return new MedalhaJpaAdapter(repository, entityManager);
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "app.gamificacao.persistencia", havingValue = "memoria")
    static class PersistenciaEmMemoria {

        @Bean
        GamificacaoEmMemoria gamificacaoEmMemoria() {
            return new GamificacaoEmMemoria();
        }
    }
}
