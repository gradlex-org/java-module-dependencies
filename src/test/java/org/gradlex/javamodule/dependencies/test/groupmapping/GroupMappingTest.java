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

package org.gradlex.javamodule.dependencies.test.groupmapping;

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

class GroupMappingTest {

    GradleBuild build = new GradleBuild();

    @Test
    void can_map_overlapping_groups() {

        var lib2ModuleInfoFile = build.file("lib-b/src/main/java/module-info.java");
        var lib2BuildFile = build.file("lib-b/build.gradle.kts").writeText(build.libBuildFile.text());
        build.settingsFile.appendText("include(\"lib-b\")");

        build.libModuleInfoFile.appendText("module com.lib { }");
        build.libBuildFile.appendText("group = \"com.foo\"");
        lib2ModuleInfoFile.appendText("module com.example.lib.b { }");
        lib2BuildFile.appendText("group = \"com.example\"");
        build.appModuleInfoFile.appendText("""         
            module org.gradlex.test.app {
                requires com.lib;
                requires com.example.lib.b;
            }""");
        build.appBuildFile.appendText(""" 
            javaModuleDependencies {
                moduleNamePrefixToGroup.put("com.", "com.foo")
                moduleNamePrefixToGroup.put("com.example.", "com.example")
            }""");

        var result = build.runner(false, "assemble").build().task(":app:compileJava");

        assertThat(result).isNotNull();
        assertThat(result.getOutcome()).isEqualTo(SUCCESS);
    }

}
