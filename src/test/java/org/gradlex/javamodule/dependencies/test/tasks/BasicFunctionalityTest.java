/*
 * Copyright the GradleX team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradlex.javamodule.dependencies.test.tasks;

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
            generateTestModuleInfoFile - Generate 'module-info.java' in 'test' source set"""
        );
    }

}
