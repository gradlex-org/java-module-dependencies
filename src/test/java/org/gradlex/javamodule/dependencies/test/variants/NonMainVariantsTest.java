// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.test.variants;

import static org.assertj.core.api.Assertions.assertThat;

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild;
import org.junit.jupiter.api.Test;

class NonMainVariantsTest {

    GradleBuild build = new GradleBuild();

    @Test
    void finds_test_fixtures_module() {
        build.appModuleInfoFile.writeText(
                """
            module org.gradlex.test.app {
                requires org.gradlex.test.lib.test.fixtures;
            }""");
        build.libModuleInfoFile.writeText("""
            module org.gradlex.test.lib {
            }""");
        build.file("lib/src/testFixtures/java/module-info.java")
                .writeText("""
            module org.gradlex.test.lib.test.fixtures {
            }""");

        var result = build.printRuntimeJars();

        assertThat(result.getOutput()).contains("[lib-test-fixtures.jar, lib.jar]");
    }

    @Test
    void finds_feature_variant_module() {
        build.libBuildFile.appendText(
                """
            val extraFeature = sourceSets.create("extraFeature")
            java.registerFeature(extraFeature.name) {
                usingSourceSet(extraFeature)
            }""");

        build.appModuleInfoFile.writeText(
                """
            module org.gradlex.test.app {
                requires org.gradlex.test.lib.extra.feature;
            }""");
        build.libModuleInfoFile.writeText("""
            module org.gradlex.test.lib {
            }""");
        build.file("lib/src/extraFeature/java/module-info.java")
                .appendText("""
            module org.gradlex.test.lib.extra.feature {
            }""");

        var result = build.printRuntimeJars();

        assertThat(result.getOutput()).contains("[lib-extra-feature.jar");
    }

    @Test
    void finds_published_feature_variant_when_corresponding_mapping_is_defined() {
        // There are no modules published like this anywhere public right now.
        // We test that the expected Jar file would have been downloaded if "org.slf4j" would have test fixtures.
        build.appBuildFile.appendText(
                """
            javaModuleDependencies {
                moduleNameToGA.put("org.slf4j.test.fixtures", "org.slf4j:slf4j-api|org.slf4j:slf4j-api-test-fixtures")
            }
            dependencies.constraints {
                javaModuleDependencies { implementation(gav("org.slf4j", "2.0.3")) }
            }""");
        build.appModuleInfoFile.appendText(
                """
            module org.gradlex.test.app {
                requires org.slf4j.test.fixtures;
            }""");

        var result = build.fail();

        assertThat(result.getOutput())
                .contains("Unable to find a variant", "requested capability", "org.slf4j:slf4j-api-test-fixtures");
    }
}
