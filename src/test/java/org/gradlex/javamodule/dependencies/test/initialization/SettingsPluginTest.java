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
import org.junit.jupiter.api.Test;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.NO_SOURCE;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

class SettingsPluginTest {

    GradleBuild build = new GradleBuild();

    @BeforeEach
    void setup() {
        build.settingsFile.writeText("plugins { id(\"org.gradlex.java-module-dependencies\") }");
        build.appBuildFile.delete();
        build.libBuildFile.delete();
    }

    @Test
    void can_define_individual_modules() {
        build.settingsFile.appendText("""
            javaModules {
                module("app") { plugin("application") }
                module("lib") {
                    plugin("java-library")
                    artifact = "lib-x"
                }
            }""");
        build.libModuleInfoFile.writeText("module abc.lib { }");
        build.appModuleInfoFile.writeText("""
            module org.gradlex.test.app {
                requires abc.lib;
            }""");

        var result = build.runner(":app:compileJava").build();

        assertThat(requireNonNull(result.task(":app:compileJava")).getOutcome()).isEqualTo(SUCCESS);
        assertThat(requireNonNull(result.task(":lib-x:compileJava")).getOutcome()).isEqualTo(SUCCESS);
    }

    @Test
    void finds_all_modules_in_a_directory() {
        build.settingsFile.appendText("""
            javaModules {
                directory(".") { plugin("java-library") }
            }""");
        build.libModuleInfoFile.writeText("module abc.lib { }");
        build.appModuleInfoFile.writeText("""
            module org.gradlex.test.app {
                requires abc.lib;
            }""");

        var result = build.runner(":app:build").build();

        assertThat(requireNonNull(result.task(":app:compileJava")).getOutcome()).isEqualTo(SUCCESS);
        assertThat(requireNonNull(result.task(":lib:compileJava")).getOutcome()).isEqualTo(SUCCESS);
    }

    @Test
    void configurationCacheHit() {
        build.settingsFile.appendText("""
            javaModules {
                directory(".") { plugin("java-library") }
            }""");
        build.libModuleInfoFile.writeText("module abc.lib { }");
        build.appModuleInfoFile.writeText("""
            module org.gradlex.test.app {
                requires abc.lib;
            }""");


        var runner = build.runner(":app:compileJava");
        var result = runner.build();

        assertThat(result.getOutput()).contains("Calculating task graph as no cached configuration is available for tasks: :app:compileJava");

        result = runner.build();

        assertThat(result.getOutput()).contains("Reusing configuration cache.");
    }

    @Test
    void configurationCacheHitExtraDir() {
        build.settingsFile.appendText("""
            javaModules {
                directory(".") { plugin("java-library") }
            }""");
        build.libModuleInfoFile.writeText("module abc.lib { }");
        build.appModuleInfoFile.writeText("""
            module org.gradlex.test.app {
                requires abc.lib;
            }""");

        var runner = build.runner(":app:compileJava");
        var result = runner.build();

        assertThat(result.getOutput()).contains("Calculating task graph as no cached configuration is available for tasks: :app:compileJava");

        build.projectDir.dir(".thisShallBeIgnored");
        result = runner.build();

        assertThat(result.getOutput()).contains("Reusing configuration cache.");
    }

    @Test
    void configurationCacheHitExtraNotIgnored() {
        build.settingsFile.appendText("""
            javaModules {
                directory(".") { plugin("java-library") }
            }""");
        build.libModuleInfoFile.writeText("module abc.lib { }");
        build.appModuleInfoFile.writeText("""
            module org.gradlex.test.app {
                requires abc.lib;
            }""");

        var runner = build.runner(":app:compileJava");
        var result = runner.build();

        assertThat(result.getOutput()).contains("Calculating task graph as no cached configuration is available for tasks: :app:compileJava");

        build.projectDir.dir("thisShallNotBeIgnored");
        result = runner.build();

        assertThat(result.getOutput()).contains("Calculating task graph as configuration cache cannot be reused because a build logic input of type 'ValueModuleDirectoryListing' has changed.");
    }

    @Test
    void configurationCacheHitIrrelevantChange() {
        build.settingsFile.appendText("""
            javaModules {
                directory(".") { plugin("java-library") }
            }""");
        build.libModuleInfoFile.writeText("module abc.lib { }");
        build.appModuleInfoFile.writeText("""
            module org.gradlex.test.app {
                requires abc.lib;
            }""");

        var runner = build.runner(":app:compileJava");
        var result = runner.build();

        assertThat(result.getOutput()).contains("Calculating task graph as no cached configuration is available for tasks: :app:compileJava");

        build.appModuleInfoFile.writeText("""
            module org.gradlex.test.app {
                requires abc.lib; //This is a comment and should not break the configurationCache
            }""");
        result = runner.build();

        assertThat(result.getOutput()).contains("Reusing configuration cache.");
    }

    @Test
    void configurationCacheMissRelevantChange() {
        build.settingsFile.appendText("""
            javaModules {
                directory(".") { plugin("java-library") }
            }""");
        build.libModuleInfoFile.writeText("module abc.lib { }");
        build.appModuleInfoFile.writeText("""
            module org.gradlex.test.app {
                requires abc.lib;
            }""");

        var runner = build.runner(":app:compileJava");
        var result = runner.build();

        assertThat(result.getOutput()).contains("Calculating task graph as no cached configuration is available for tasks: :app:compileJava");

        build.appModuleInfoFile.writeText("""
            module org.gradlex.test.app {
               //dependency removed; so thats indeed a configuration change
            }
        """);
        result = runner.build();

        assertThat(result.getOutput()).contains("Calculating task graph as configuration cache cannot be reused because a build logic input of type 'ValueSourceModuleInfo' has changed.\n");
    }

    @Test
    void automatically_sets_module_for_application_plugin() {
        build.settingsFile.appendText("""
            javaModules {
                directory(".") {
                    plugin("java-library")
                    module("app") { plugin("application") }
                }
            }""");
        build.libModuleInfoFile.writeText("module abc.libxyz { }");
        build.appModuleInfoFile.writeText("""
            module org.gradlex.test.app {
                requires abc.libxyz;
            }""");
        build.appBuildFile.appendText("application.mainClass = \"app.App\"");
        build.file("app/src/main/java/app/App.java").writeText("package app; public class App { public static void main(String[] args) { } }");

        var result = build.runner(":app:run").build();

        assertThat(requireNonNull(result.task(":app:run")).getOutcome()).isEqualTo(SUCCESS);
        assertThat(requireNonNull(result.task(":app:compileJava")).getOutcome()).isEqualTo(SUCCESS);
        assertThat(requireNonNull(result.task(":lib:compileJava")).getOutcome()).isEqualTo(SUCCESS);
    }

    @Test
    void can_depend_on_test_fixtures_module() {
        build.settingsFile.appendText("""
            javaModules {
                directory(".") {
                    group = "bar.foo"
                    plugin("java-library")
                    plugin("java-test-fixtures")
                }
            }""");
        build.libModuleInfoFile.writeText("module foo.bar.m { }");
        build.file("lib/src/testFixtures/java/module-info.java").writeText("module abc.libxyz.dsdsds { }");
        build.appModuleInfoFile.writeText("""
            module org.gradlex.test.app {
                requires foo.bar.m;
                requires abc.libxyz.dsdsds;
            }""");

        var result = build.runner(":app:compileJava").build();

        assertThat(requireNonNull(result.task(":app:compileJava")).getOutcome()).isEqualTo(SUCCESS);
        assertThat(requireNonNull(result.task(":lib:compileJava")).getOutcome()).isEqualTo(SUCCESS);
        assertThat(requireNonNull(result.task(":lib:compileTestFixturesJava")).getOutcome()).isEqualTo(SUCCESS);
    }

    @Test
    void can_apply_root_project_plugin_from_settings() {
        build.settingsFile.appendText("""
            rootPlugins {
                id("java")
            }""");

        var result = build.runner(":compileJava").build();

        assertThat(requireNonNull(result.task(":compileJava")).getOutcome()).isEqualTo(NO_SOURCE);
    }

    @Test
    void can_have_moduleinfo_in_custom_location() {
        build.settingsFile.appendText("""
            javaModules {
                module("app") { plugin("application") }
                module("lib") {
                    plugin("java-library")
                    moduleInfoPaths.add("src")
                }
            }""");
        build.libBuildFile.appendText("sourceSets.main { java.setSrcDirs(listOf(\"src\")) }");
        build.file("lib/src/module-info.java").writeText("module abc.lib { }");
        build.appModuleInfoFile.writeText("""
            module org.gradlex.test.app {
                requires abc.lib;
            }""");

        var result = build.runner(":app:compileJava").build();

        assertThat(requireNonNull(result.task(":app:compileJava")).getOutcome()).isEqualTo(SUCCESS);
        assertThat(requireNonNull(result.task(":lib:compileJava")).getOutcome()).isEqualTo(SUCCESS);
    }

}
