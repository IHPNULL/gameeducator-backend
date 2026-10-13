package org.facens.grupo_6_gameeducator.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Fitness functions das decisoes ADR-B1-001 (monolito modular) e ADR-B1-002 (hexagonal na gamificacao).
 * Rodam no "mvn verify": uma dependencia que viole a fronteira quebra o build e bloqueia o deploy (ADR-B1-006).
 */
@AnalyzeClasses(packages = ArquiteturaModularTest.BASE, importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaModularTest {

    static final String BASE = "org.facens.grupo_6_gameeducator";
    private static final String GAMIFICACAO = BASE + ".gamificacao";

    @ArchTest
    static final ArchRule dominioDaGamificacaoEPuro = noClasses()
            .that().resideInAPackage(GAMIFICACAO + ".dominio..")
            .should().dependOnClassesThat().resideOutsideOfPackages(GAMIFICACAO + ".dominio..", "java..")
            .as("D2: o dominio de gamificacao nao depende de Spring, JPA nem de outros pacotes do Core");

    @ArchTest
    static final ArchRule aplicacaoNaoConheceInfraestrutura = noClasses()
            .that().resideInAPackage(GAMIFICACAO + ".aplicacao..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    GAMIFICACAO + ".adaptador..", "org.springframework..", "jakarta.persistence..")
            .as("D2: o caso de uso depende de portas, nunca de adaptadores, Spring ou JPA");

    @ArchTest
    static final ArchRule outrosModulosSoUsamAApiPublica = noClasses()
            .that().resideOutsideOfPackage(GAMIFICACAO + "..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    GAMIFICACAO + ".dominio..", GAMIFICACAO + ".aplicacao..", GAMIFICACAO + ".porta..",
                    GAMIFICACAO + ".adaptador..", GAMIFICACAO + ".config..")
            .as("D1: fora do modulo, so a GamificacaoApi e seus records sao visiveis");

    @ArchTest
    static final ArchRule gamificacaoNaoDependeDeWebNemServices = noClasses()
            .that().resideInAPackage(GAMIFICACAO + "..")
            .should().dependOnClassesThat().resideInAnyPackage(BASE + ".web..", BASE + ".service..")
            .as("D1: o modulo de gamificacao nao conhece quem o chama (sem ciclo)");

    @ArchTest
    static final ArchRule servicesNaoConhecemOClienteHttpDaIa = noClasses()
            .that().resideOutsideOfPackage(BASE + ".ia..")
            .should().dependOnClassesThat().resideInAnyPackage(BASE + ".ia.adaptador..", BASE + ".ia.config..")
            .as("D3: o Core depende da porta ServicoDeIa, nunca do cliente HTTP/disjuntor");

    @ArchTest
    static final ArchRule servicesSoConhecemAPortaDeEventos = noClasses()
            .that().resideOutsideOfPackage(BASE + ".eventos..")
            .should().dependOnClassesThat().resideInAnyPackage(BASE + ".eventos.adaptador..", BASE + ".eventos.config..", BASE + ".eventos.outbox..")
            .as("D4: quem emite eventos depende da porta EmissorDeEventos, nunca do RabbitMQ/REST");

    @ArchTest
    static final ArchRule dominioIotNaoConheceOBroker = noClasses()
            .that().resideInAPackage(BASE + ".iot")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.amqp..", "org.eclipse.paho..")
            .as("D5: so o adaptador IoT conhece AMQP; o Core nunca fala MQTT");

    @ArchTest
    static final ArchRule semCiclosEntrePacotes = slices()
            .matching(BASE + ".(*)..")
            .should().beFreeOfCycles()
            .as("D1: os pacotes de primeiro nivel do Core nao formam ciclos");

    @ArchTest
    static final ArchRule camadasDoLegadoRespeitadas = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Web").definedBy(BASE + ".web..")
            .layer("Service").definedBy(BASE + ".service..")
            .layer("Repository").definedBy(BASE + ".repository..")
            .layer("Gamificacao").definedBy(GAMIFICACAO + "..")
            .layer("IntegracaoIa").definedBy(BASE + ".ia..")
            .layer("Seed").definedBy(BASE + ".config..")
            .whereLayer("Web").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Web", "Seed")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service", "Seed")
            .whereLayer("Gamificacao").mayOnlyBeAccessedByLayers("Web", "Service", "Seed")
            .whereLayer("IntegracaoIa").mayOnlyBeAccessedByLayers("Web", "Service")
            .as("AC1 + D1: web -> service -> repository; gamificacao acessada so pela API");
}
