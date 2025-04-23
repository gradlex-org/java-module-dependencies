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

package org.gradlex.javamodule.dependencies.test;

import org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES;
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES_RUNTIME;
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES_TRANSITIVE;
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES_STATIC;
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES_STATIC_TRANSITIVE;

class ModuleInfoParseTest {

    @Test
    void ignores_single_line_comments() {
        var moduleInfo = new ModuleInfo("""
            module some.thing {
                // requires com.bla.blub;
                requires transitive foo.bar.la;
            }""");

        assertThat(moduleInfo.moduleNamePrefix("thing", "main", false)).isEqualTo("some");
        assertThat(moduleInfo.get(REQUIRES)).isEmpty();
        assertThat(moduleInfo.get(REQUIRES_TRANSITIVE)).containsExactly("foo.bar.la");
        assertThat(moduleInfo.get(REQUIRES_STATIC)).isEmpty();
        assertThat(moduleInfo.get(REQUIRES_STATIC_TRANSITIVE)).isEmpty();
        assertThat(moduleInfo.get(REQUIRES_RUNTIME)).isEmpty();
    }

    @Test
    void ignores_single_line_comments_late_in_line() {
        var moduleInfo = new ModuleInfo("""
            module some.thing { // module some.thing.else
                requires transitive foo.bar.la;
            }""");

        assertThat(moduleInfo.moduleNamePrefix("thing", "main", false)).isEqualTo("some");
        assertThat(moduleInfo.get(REQUIRES)).isEmpty();
        assertThat(moduleInfo.get(REQUIRES_TRANSITIVE)).containsExactly("foo.bar.la");
        assertThat(moduleInfo.get(REQUIRES_STATIC)).isEmpty();
        assertThat(moduleInfo.get(REQUIRES_STATIC_TRANSITIVE)).isEmpty();
        assertThat(moduleInfo.get(REQUIRES_RUNTIME)).isEmpty();
    }

    @Test
    void ignores_multi_line_comments() {
        var moduleInfo = new ModuleInfo("""
            module some.thing {
                /* requires com.bla.blub;
                requires transitive foo.bar.la;
                */
                requires static foo.bar.la;
            }
        """);

        assertThat(moduleInfo.get(REQUIRES)).isEmpty();
        assertThat(moduleInfo.get(REQUIRES_TRANSITIVE)).isEmpty();
        assertThat(moduleInfo.get(REQUIRES_STATIC)).containsExactly("foo.bar.la");
        assertThat(moduleInfo.get(REQUIRES_STATIC_TRANSITIVE)).isEmpty();
        assertThat(moduleInfo.get(REQUIRES_RUNTIME)).isEmpty();
    }

    @Test
    void ignores_multi_line_comments_between_keywords() {
        var moduleInfo = new ModuleInfo("""
            module some.thing {
                /*odd comment*/ requires transitive foo.bar.la;
                requires/* weird comment*/ static foo.bar.lo;
                requires /*something to say*/foo.bar.li; /*
                    requires only.a.comment
                */
            }""");

        assertThat(moduleInfo.get(REQUIRES)).containsExactly("foo.bar.li");
        assertThat(moduleInfo.get(REQUIRES_TRANSITIVE)).containsExactly("foo.bar.la");
        assertThat(moduleInfo.get(REQUIRES_STATIC)).containsExactly("foo.bar.lo");
        assertThat(moduleInfo.get(REQUIRES_STATIC_TRANSITIVE)).isEmpty();
        assertThat(moduleInfo.get(REQUIRES_RUNTIME)).isEmpty();
    }

    @Test
    void supports_runtime_dependencies_through_special_keyword() {
        var moduleInfo = new ModuleInfo("""
            module some.thing {
                requires /*runtime*/ foo.bar.lo;
            }
        """);

        assertThat(moduleInfo.get(REQUIRES)).isEmpty();
        assertThat(moduleInfo.get(REQUIRES_TRANSITIVE)).isEmpty();
        assertThat(moduleInfo.get(REQUIRES_STATIC)).isEmpty();
        assertThat(moduleInfo.get(REQUIRES_STATIC_TRANSITIVE)).isEmpty();
        assertThat(moduleInfo.get(REQUIRES_RUNTIME)).containsExactly("foo.bar.lo");
    }
}
