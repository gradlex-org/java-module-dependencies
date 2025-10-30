// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.test.initialization;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("no-cross-version")
class SettingsPluginVersionManagementTest {

    GradleBuild build = new GradleBuild();

    @BeforeEach
    void setup() {
        var buildFile =
                """
            plugins { id("java-library") }
            dependencies { implementation(platform(project(":versions"))) }""";
        build.settingsFile.writeText(
                """
            plugins { id("org.gradlex.java-module-dependencies") }
            dependencyResolutionManagement { repositories.mavenCentral() }
            """);
        build.appBuildFile.writeText(buildFile);
        build.libBuildFile.writeText(buildFile);
    }

    @Test
    void can_define_a_version_providing_project_in_settings() {
        build.settingsFile.appendText(
                """
            javaModules {
                directory(".")
                versions("gradle/versions")
            }""");
        build.libModuleInfoFile.writeText("module abc.lib { }");
        build.appModuleInfoFile.writeText(
                """
            module org.gradlex.test.app {
                requires abc.lib;
                requires java.inject;
            }""");
        build.file("gradle/versions/build.gradle.kts")
                .writeText(
                        """
                moduleInfo {
                    version("java.inject", "1.0.5")
                }""");

        var result = build.runner(":app:compileJava").build();

        assertThat(requireNonNull(result.task(":app:compileJava")).getOutcome()).isEqualTo(SUCCESS);
    }

    @Test
    void can_define_a_version_providing_project_in_settings_with_additional_plugin() {
        build.settingsFile.appendText(
                """
            javaModules {
                directory(".")
                versions("gradle/versions") { plugin("maven-publish") }
            }""");
        build.libModuleInfoFile.writeText("module abc.lib { }");
        build.appModuleInfoFile.writeText(
                """
            module org.gradlex.test.app {
                requires abc.lib;
                requires java.inject;
            }""");
        build.file("gradle/versions/build.gradle.kts")
                .writeText(
                        """
                moduleInfo {
                    version("java.inject", "1.0.5")
                }""");

        var result = build.runner(":versions:publish").build();

        assertThat(requireNonNull(result.task(":versions:publish")).getOutcome())
                .isEqualTo(UP_TO_DATE);
    }
}
