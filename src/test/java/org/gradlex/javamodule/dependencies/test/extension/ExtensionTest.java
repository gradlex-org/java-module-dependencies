// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.test.extension;

import static org.assertj.core.api.Assertions.assertThat;

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild;
import org.junit.jupiter.api.Test;

class ExtensionTest {

    GradleBuild build = new GradleBuild();

    @Test
    void can_access_mapping_information_from_extension() {
        build.appBuildFile.appendText("""
            javaModuleDependencies.moduleNamePrefixToGroup.put("org.example.app.", "org.example.gr")

            javaModuleDependencies {
                println(ga("com.fasterxml.jackson.core").get())
                println(ga(provider { "com.fasterxml.jackson.databind" }).get())
                println(gav("com.fasterxml.jackson.core", "1.0").get())
                println(gav(provider { "com.fasterxml.jackson.databind" }, provider { "1.0" }).get())
                println(moduleName("com.fasterxml.jackson.core:jackson-core").get())
                println(moduleName(provider { "com.fasterxml.jackson.core:jackson-databind" }).get())

                println(ga("org.example.app.my.mod1").get())
                println(ga(provider { "org.example.app.my.mod2.impl" }).get())
                println(gav("org.example.app.my.mod3.impl.int", "1.0").get())
                println(gav(provider { "org.example.app.mod4" }, provider { "1.0" }).get())
                println(moduleName("org.example.gr:mod8.ab").get())
                println(moduleName(provider { "org.example.gr:mod.z7.i9" }).get())
            }""");

        var result = build.build();

        assertThat(result.getOutput()).contains("""
            com.fasterxml.jackson.core:jackson-core
            com.fasterxml.jackson.core:jackson-databind
            com.fasterxml.jackson.core:jackson-core:1.0
            com.fasterxml.jackson.core:jackson-databind:1.0
            com.fasterxml.jackson.core
            com.fasterxml.jackson.databind
            org.example.gr:my.mod1
            org.example.gr:my.mod2.impl
            org.example.gr:my.mod3.impl.int:1.0
            org.example.gr:mod4:1.0
            org.example.app.mod8.ab
            org.example.app.mod.z7.i9""");
    }
}
