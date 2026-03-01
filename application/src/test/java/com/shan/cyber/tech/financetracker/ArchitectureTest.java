package com.shan.cyber.tech.financetracker;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "com.shan.cyber.tech.financetracker",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    // Core hexagonal rule: domain must be framework-free
    @ArchTest
    static final ArchRule domain_must_not_import_spring =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "com.fasterxml.jackson..",
                            "lombok..")
                    .as("Domain classes must not depend on Spring, JPA, Jackson, or Lombok");

    // Controllers call inbound ports, not domain services
    @ArchTest
    static final ArchRule controllers_must_not_call_domain_services =
            noClasses()
                    .that().resideInAPackage("..adapter.inbound.web..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..domain.service..")
                    .allowEmptyShould(true)
                    .as("Controllers must call inbound ports, not domain services directly");

    // Cross-context boundary rules (allowEmptyShould since contexts are scaffolded but empty)
    @ArchTest
    static final ArchRule account_domain_must_not_import_transaction_domain =
            noClasses()
                    .that().resideInAPackage("..account.domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..transaction.domain..")
                    .allowEmptyShould(true)
                    .as("Account domain must not depend on Transaction domain");

    @ArchTest
    static final ArchRule transaction_domain_must_not_import_account_domain =
            noClasses()
                    .that().resideInAPackage("..transaction.domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..account.domain..")
                    .allowEmptyShould(true)
                    .as("Transaction domain must not depend on Account domain");

    @ArchTest
    static final ArchRule budget_domain_must_not_import_transaction_domain =
            noClasses()
                    .that().resideInAPackage("..budget.domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..transaction.domain..")
                    .allowEmptyShould(true)
                    .as("Budget domain must not depend on Transaction domain");

    @ArchTest
    static final ArchRule identity_domain_must_not_import_other_domains =
            noClasses()
                    .that().resideInAPackage("..identity.domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..account.domain..",
                            "..transaction.domain..",
                            "..budget.domain..",
                            "..category.domain..",
                            "..reporting.domain..")
                    .allowEmptyShould(true)
                    .as("Identity domain must not depend on any other bounded context domain");
}
