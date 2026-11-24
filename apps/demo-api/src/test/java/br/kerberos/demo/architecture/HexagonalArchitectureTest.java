package br.kerberos.demo.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Regras da seção 6 do CLAUDE.md. O build falha se alguma for quebrada. */
@AnalyzeClasses(packages = "br.kerberos.demo", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule dominioNaoDependeDeFramework = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "com.fasterxml..", "tools.jackson..", "org.keycloak..")
            .because("o domínio é Java puro");

    @ArchTest
    static final ArchRule dominioNaoConheceCamadasExternas = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..adapter..", "..config..");

    @ArchTest
    static final ArchRule aplicacaoNaoDependeDeAdaptersNemFramework = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter..", "..config..", "org.springframework..")
            .because("casos de uso só conhecem portas e domínio");

    @ArchTest
    static final ArchRule adaptersNaoSeChamamEntreSi = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out..")
            .because("adapters conversam pelos casos de uso, nunca diretamente");
}
