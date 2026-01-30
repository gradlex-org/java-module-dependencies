// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.test.groupmapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild;
import org.junit.jupiter.api.Test;

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
