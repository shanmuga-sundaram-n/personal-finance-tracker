package com.shan.cyber.tech.financetracker;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.conditions.ArchConditions.dependOnClassesThat;
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

    // Rule 1: Reporting context domain isolation — reporting.domain must not import from other context domains
    @ArchTest
    static final ArchRule reporting_domain_must_not_import_other_context_domains =
            noClasses()
                    .that().resideInAPackage("..reporting.domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..account.domain..",
                            "..budget.domain..",
                            "..transaction.domain..",
                            "..identity.domain..")
                    .allowEmptyShould(true)
                    .as("Reporting domain must not depend on account, budget, transaction, or identity domain");

    // Rule 2: Adapters that call into other bounded contexts must live in adapter.outbound.crosscontext.
    // Each bounded context's non-crosscontext adapters must not depend on another context's inbound ports.
    @ArchTest
    static final ArchRule account_adapters_outside_crosscontext_must_not_call_other_context_inbound_ports =
            noClasses()
                    .that().resideInAPackage("..account.adapter..")
                    .and().haveSimpleNameEndingWith("Adapter")
                    .and().resideOutsideOfPackage("..adapter.outbound.crosscontext..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..transaction.domain.port.inbound..",
                            "..budget.domain.port.inbound..",
                            "..category.domain.port.inbound..",
                            "..identity.domain.port.inbound..",
                            "..reporting.domain.port.inbound..")
                    .allowEmptyShould(true)
                    .as("Account adapters outside crosscontext must not call other bounded contexts' inbound ports");

    @ArchTest
    static final ArchRule transaction_adapters_outside_crosscontext_must_not_call_other_context_inbound_ports =
            noClasses()
                    .that().resideInAPackage("..transaction.adapter..")
                    .and().haveSimpleNameEndingWith("Adapter")
                    .and().resideOutsideOfPackage("..adapter.outbound.crosscontext..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..account.domain.port.inbound..",
                            "..budget.domain.port.inbound..",
                            "..category.domain.port.inbound..",
                            "..identity.domain.port.inbound..",
                            "..reporting.domain.port.inbound..")
                    .allowEmptyShould(true)
                    .as("Transaction adapters outside crosscontext must not call other bounded contexts' inbound ports");

    @ArchTest
    static final ArchRule budget_adapters_outside_crosscontext_must_not_call_other_context_inbound_ports =
            noClasses()
                    .that().resideInAPackage("..budget.adapter..")
                    .and().haveSimpleNameEndingWith("Adapter")
                    .and().resideOutsideOfPackage("..adapter.outbound.crosscontext..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..account.domain.port.inbound..",
                            "..transaction.domain.port.inbound..",
                            "..category.domain.port.inbound..",
                            "..identity.domain.port.inbound..",
                            "..reporting.domain.port.inbound..")
                    .allowEmptyShould(true)
                    .as("Budget adapters outside crosscontext must not call other bounded contexts' inbound ports");

    @ArchTest
    static final ArchRule category_adapters_outside_crosscontext_must_not_call_other_context_inbound_ports =
            noClasses()
                    .that().resideInAPackage("..category.adapter..")
                    .and().haveSimpleNameEndingWith("Adapter")
                    .and().resideOutsideOfPackage("..adapter.outbound.crosscontext..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..account.domain.port.inbound..",
                            "..transaction.domain.port.inbound..",
                            "..budget.domain.port.inbound..",
                            "..identity.domain.port.inbound..",
                            "..reporting.domain.port.inbound..")
                    .allowEmptyShould(true)
                    .as("Category adapters outside crosscontext must not call other bounded contexts' inbound ports");

    @ArchTest
    static final ArchRule identity_adapters_outside_crosscontext_must_not_call_other_context_inbound_ports =
            noClasses()
                    .that().resideInAPackage("..identity.adapter..")
                    .and().haveSimpleNameEndingWith("Adapter")
                    .and().resideOutsideOfPackage("..adapter.outbound.crosscontext..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..account.domain.port.inbound..",
                            "..transaction.domain.port.inbound..",
                            "..budget.domain.port.inbound..",
                            "..category.domain.port.inbound..",
                            "..reporting.domain.port.inbound..")
                    .allowEmptyShould(true)
                    .as("Identity adapters outside crosscontext must not call other bounded contexts' inbound ports");

    @ArchTest
    static final ArchRule reporting_adapters_outside_crosscontext_must_not_call_other_context_inbound_ports =
            noClasses()
                    .that().resideInAPackage("..reporting.adapter..")
                    .and().haveSimpleNameEndingWith("Adapter")
                    .and().resideOutsideOfPackage("..adapter.outbound.crosscontext..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..account.domain.port.inbound..",
                            "..transaction.domain.port.inbound..",
                            "..budget.domain.port.inbound..",
                            "..category.domain.port.inbound..",
                            "..identity.domain.port.inbound..")
                    .allowEmptyShould(true)
                    .as("Reporting adapters outside crosscontext must not call other bounded contexts' inbound ports");

    // Rule 3: Controllers must not import non-shared domain model types directly
    @ArchTest
    static final ArchRule controllers_must_not_import_non_shared_domain_model =
            noClasses()
                    .that().resideInAPackage("..adapter.inbound.web..")
                    .and().haveSimpleNameEndingWith("Controller")
                    .should(dependOnClassesThat(
                            resideInAPackage("..domain.model..")
                                    .and(not(resideInAPackage("..shared.domain.model..")))))
                    .allowEmptyShould(true)
                    .as("Controllers must not import non-shared domain model types; use inbound ports or shared.domain.model");

    // Rule 4: Category domain isolation — category.domain must not import from transaction or budget domain
    @ArchTest
    static final ArchRule category_domain_must_not_import_transaction_or_budget_domain =
            noClasses()
                    .that().resideInAPackage("..category.domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..transaction.domain..",
                            "..budget.domain..")
                    .allowEmptyShould(true)
                    .as("Category domain must not depend on transaction or budget domain");
}
