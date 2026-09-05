package com.aios.authz.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the two layering rules docs/ARCHITECTURE.md §3 states explicitly:
 * {@code domain} depends on nothing else in the project, and {@code policy}
 * (including {@code policy.rules}) never depends on {@code engine} or
 * {@code api}. Not a full seven-layer pyramid check — just the two properties
 * that matter for keeping the policy layer swappable for OPA/Cedar later
 * (ADR-0002) and the domain model framework-free (ADR-0001).
 *
 * <p>Package patterns are rooted at {@code com.aios.authz.*}, not the looser
 * {@code ..api..} form — that wildcard matches any package with an "api"
 * segment anywhere, which false-positives on third-party packages like
 * {@code org.assertj.core.api} used throughout the test sources this rule also
 * scans.
 */
@AnalyzeClasses(packages = "com.aios.authz")
class LayeringTest {

    private static final String ROOT = "com.aios.authz";

    @ArchTest
    static final ArchRule domain_depends_on_nothing_else_in_the_project =
        noClasses().that().resideInAPackage(ROOT + ".domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                ROOT + ".provenance..", ROOT + ".state..", ROOT + ".policy..", ROOT + ".engine..",
                ROOT + ".harness..", ROOT + ".api..", ROOT + ".config..");

    @ArchTest
    static final ArchRule policy_does_not_depend_on_engine_or_api =
        noClasses().that().resideInAPackage(ROOT + ".policy..")
            .should().dependOnClassesThat().resideInAnyPackage(ROOT + ".engine..", ROOT + ".api..");
}
