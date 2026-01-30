// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.test.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild;
import org.junit.jupiter.api.Test;

class BasicFunctionalityTest {

    GradleBuild build = new GradleBuild(true);

    @Test
    void can_configure_all_tasks_in_a_build_without_error() {
        build.libModuleInfoFile.writeText("module abc.lib { }");
        build.appModuleInfoFile.writeText("""
            module org.gradlex.test.app {
                requires abc.lib;
            }
        """);

        var result = build.runner("tasks").build();

        assertThat(result.getOutput()).contains("""
            Java modules tasks
            ------------------
            checkModuleInfo - Check order of directives in 'module-info.java' in 'main' source set
            checkTestFixturesModuleInfo - Check order of directives in 'module-info.java' in 'testFixtures' source set
            checkTestModuleInfo - Check order of directives in 'module-info.java' in 'test' source set
            generateAllModuleInfoFiles - Generate 'module-info.java' files in all source sets
            generateBuildFileDependencies - Generate 'dependencies' block in 'build.gradle.kts'
            generateCatalog - Generate 'libs.versions.toml' file
            generateModuleInfoFile - Generate 'module-info.java' in 'main' source set
            generateTestFixturesModuleInfoFile - Generate 'module-info.java' in 'testFixtures' source set
            generateTestModuleInfoFile - Generate 'module-info.java' in 'test' source set""");
    }
}
