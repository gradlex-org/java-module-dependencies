// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.test.localmodules;

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild;
import org.junit.jupiter.api.Test;

class LocalModuleMappingsTest {

    GradleBuild build = new GradleBuild();

    @Test
    void automatically_maps_local_modules_if_name_prefix_matches() {
        build.libModuleInfoFile.writeText("module org.gradlex.test.lib { }");
        build.appModuleInfoFile.writeText(
                """
            module org.gradlex.test.app {
                requires org.gradlex.test.lib;
            }""");

        build.build();
    }

    @Test
    void automatically_maps_local_modules_if_name_matches() {
        build.libModuleInfoFile.writeText("module lib { }");
        build.appModuleInfoFile.writeText("""
            module app {
                requires lib;
            }""");

        build.build();
    }

    @Test
    void a_prefix_to_group_mapping_can_be_used() {
        build.appBuildFile.appendText(
                """
            javaModuleDependencies.moduleNamePrefixToGroup.put("abc.", "foo.gr")
        """);
        build.libBuildFile.appendText("""
            group = "foo.gr"
        """);

        build.libModuleInfoFile.writeText("module abc.lib { }");
        build.appModuleInfoFile.writeText(
                """
            module org.gradlex.test.app {
                requires abc.lib;
            }""");

        build.runner(false, "build").build();
    }

    @Test
    void does_not_fail_if_there_are_two_project_with_same_name_but_different_path() {
        build.libModuleInfoFile.writeText(
                """
            module org.gradlex.test.lib {
                requires org.gradlex.test.anotherlib;
            }
        """);
        build.settingsFile.appendText("include(\"another:lib\")");
        build.file("another/lib/build.gradle.kts")
                .writeText(
                        """
            plugins {
                id("org.gradlex.java-module-dependencies")
                id("java-library")
            }
            group = "another"
        """);
        build.file("another/lib/src/main/java/module-info.java")
                .writeText("""
            module org.gradlex.test.anotherlib { }
        """);
        build.file("gradle/modules.properties")
                .writeText("""
            org.gradlex.test.anotherlib=another:lib
        """);

        build.runner(false, "build");
    }
}
