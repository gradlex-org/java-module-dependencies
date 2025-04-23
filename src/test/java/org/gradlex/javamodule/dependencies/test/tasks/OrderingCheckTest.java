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

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class OrderingCheckTest {

    GradleBuild build = new GradleBuild(true);

    @Test
    void order_is_expected_to_be_alphabetic_for_each_scope_individually() {
        build.appModuleInfoFile.writeText("""
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
    void if_order_is_not_alphabetic_for_a_scope_an_advice_is_given() throws IOException {
        build.appModuleInfoFile.writeText("""
            module org.example.app {
                requires a.b.c
                requires b.z.u
                requires b.f.g
            
                requires static c.w.q
                requires static c.z.u
            }""");

        var result = build.runner(":app:checkAllModuleInfo").buildAndFail();
        assertThat(result.getOutput()).contains("""
            > %s/app/src/main/java/module-info.java
             \s
              'requires' are not declared in alphabetical order. Please use this order:
                  requires a.b.c;
                  requires b.f.g;
                  requires b.z.u;""".formatted(build.projectDir.canonicalPath()));
    }

    @Test
    void own_modules_are_expected_to_go_first() {
        build.appModuleInfoFile.writeText("""
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