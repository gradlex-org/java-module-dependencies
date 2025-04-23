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
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

class SettingsPluginIncludeTest {

    GradleBuild build = new GradleBuild();

    @BeforeEach
    void setup() {
        build.settingsFile.writeText("plugins { id(\"org.gradlex.java-module-dependencies\") }");
        build.appBuildFile.delete();
        build.libBuildFile.delete();
    }

    @Test
    void can_define_included_subprojects_as_modules() {
        build.settingsFile.appendText("""
            include(":project:with:custom:path")
            javaModules {
                module(project(":project:with:custom:path")) {
                    group = "org.example"
                    plugin("java-library")
                }
                module(project(":project:with:custom")) {
                    group = "org.example"
                    plugin("java-library")
                }
            }""");

        build.file("project/with/custom/path/src/main/java/module-info.java").writeText("module abc.liba { }");
        build.file("project/with/custom/src/main/java/module-info.java").writeText("""
            module abc.libb {
                requires abc.liba;
            }""");

        var result = build.runner(":project:with:custom:compileJava").build();

        assertThat(requireNonNull(result.task(":project:with:custom:path:compileJava")).getOutcome()).isEqualTo(SUCCESS);
        assertThat(requireNonNull(result.task(":project:with:custom:compileJava")).getOutcome()).isEqualTo(SUCCESS);
    }

    @Test
    void can_define_included_subprojects_with_custom_project_directory_as_modules() {
        build.settingsFile.appendText("""
            include(":project:with:custom:path")
            project(":project:with:custom:path").projectDir = file("lib")
            project(":project:with:custom").projectDir = file("app")
            javaModules {
                module(project(":project:with:custom:path")) {
                    group = "org.example"
                    plugin("java-library")
                }
                module(project(":project:with:custom")) {
                    group = "org.example"
                    plugin("java-library")
                }
            }""");

        build.libModuleInfoFile.writeText("module abc.lib { }");
        build.appModuleInfoFile.writeText("""
            module abc.app {
                requires abc.lib;
            }""");

        var result = build.runner(":project:with:custom:jar").build();

        assertThat(requireNonNull(result.task(":project:with:custom:path:compileJava")).getOutcome()).isEqualTo(SUCCESS);
        assertThat(requireNonNull(result.task(":project:with:custom:compileJava")).getOutcome()).isEqualTo(SUCCESS);
        assertThat(build.file("lib/build/libs/path.jar").getAsPath()).exists();
        assertThat(build.file("app/build/libs/custom.jar").getAsPath()).exists();
    }

    @Test
    void projects_with_same_name_but_different_paths_are_supported() {
        build.settingsFile.appendText("""
            include(":app1:feature1:data")
            include(":app1:feature2:data")
            
            rootProject.children.forEach { appContainer ->
                appContainer.children.forEach { featureContainer ->
                    featureContainer.children.forEach { module ->
                        javaModules.module(module) { plugin("java-library") }
                    }
                }
            }""");

        build.file("app1/feature1/data/src/main/java/module-info.java").writeText("module f1x.data { }");
        build.file("app1/feature2/data/src/main/java/module-info.java").writeText("""
            module f2x.data {
                requires f1x.data;
            }""");

        var result = build.runner(":app1:feature2:data:jar").build();

        assertThat(requireNonNull(result.task(":app1:feature1:data:jar")).getOutcome()).isEqualTo(SUCCESS);
        assertThat(requireNonNull(result.task(":app1:feature2:data:jar")).getOutcome()).isEqualTo(SUCCESS);
        assertThat(build.file("app1/feature1/data/build/libs/data.jar").getAsPath()).exists();
        assertThat(build.file("app1/feature2/data/build/libs/data.jar").getAsPath()).exists();
    }
}
