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

package org.gradlex.javamodule.dependencies.test.initialization;

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;

@Tag("no-cross-version")
class SettingsPluginVersionManagementTest {

    GradleBuild build = new GradleBuild();

    @BeforeEach
    void setup() {
        var buildFile = """
            plugins { id("java-library") }
            dependencies { implementation(platform(project(":versions"))) }""";
        build.settingsFile.writeText("""
            plugins { id("org.gradlex.java-module-dependencies") }
            dependencyResolutionManagement { repositories.mavenCentral() }
            """);
        build.appBuildFile.writeText(buildFile);
        build.libBuildFile.writeText(buildFile);
    }

    @Test
    void can_define_a_version_providing_project_in_settings() {
        build.settingsFile.appendText("""
            javaModules {
                directory(".")
                versions("gradle/versions")
            }""");
        build.libModuleInfoFile.writeText("module abc.lib { }");
        build.appModuleInfoFile.writeText("""
            module org.gradlex.test.app {
                requires abc.lib;
                requires java.inject;
            }""");
        build.file("gradle/versions/build.gradle.kts").writeText("""
                moduleInfo {
                    version("java.inject", "1.0.5")
                }""");

        var result = build.runner(":app:compileJava").build();

        assertThat(requireNonNull(result.task(":app:compileJava")).getOutcome()).isEqualTo(SUCCESS);
    }

    @Test
    void can_define_a_version_providing_project_in_settings_with_additional_plugin() {
        build.settingsFile.appendText("""
            javaModules {
                directory(".")
                versions("gradle/versions") { plugin("maven-publish") }
            }""");
        build.libModuleInfoFile.writeText("module abc.lib { }");
        build.appModuleInfoFile.writeText("""
            module org.gradlex.test.app {
                requires abc.lib;
                requires java.inject;
            }""");
        build.file("gradle/versions/build.gradle.kts").writeText("""
                moduleInfo {
                    version("java.inject", "1.0.5")
                }""");

        var result = build.runner(":versions:publish").build();

        assertThat(requireNonNull(result.task(":versions:publish")).getOutcome()).isEqualTo(UP_TO_DATE);
    }

}
