// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.test.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild;
import org.junit.jupiter.api.Test;

class OrderingCheckTest {

    GradleBuild build = new GradleBuild(true);

    @Test
    void order_is_expected_to_be_alphabetic_for_each_scope_individually() {
        build.appModuleInfoFile.writeText(
                """
            module org.example.app {
                requires a.b.c
                requires b.f.g
                requires b.z.u

                requires static c.w.q
                requires static c.z.u
            }""");

        build.runner(":app:checkAllModuleInfo").build();
    }

    @Test
    void if_order_is_not_alphabetic_for_a_scope_an_advice_is_given() {
        build.appModuleInfoFile.writeText(
                """
            module org.example.app {
                requires a.b.c
                requires b.z.u
                requires b.f.g

                requires static c.w.q
                requires static c.z.u
            }""");

        var result = build.runner(":app:checkAllModuleInfo").buildAndFail();
        assertThat(result.getOutput())
                .contains(
                        """
            > %s/app/src/main/java/module-info.java
             \s
              'requires' are not declared in alphabetical order. Please use this order:
                  requires a.b.c;
                  requires b.f.g;
                  requires b.z.u;"""
                                .formatted(build.projectDir.canonicalPath()));
    }

    @Test
    void own_modules_are_expected_to_go_first() {
        build.appModuleInfoFile.writeText(
                """
            module org.example.app {
                requires org.example.h
                requires org.example.j

                requires a.b.c
                requires b.f.g
                requires z.z.u

                requires static org.example.z
                requires static c.w.q
                requires static c.z.u
            }""");

        build.runner(":app:checkAllModuleInfo").build();
    }
}
