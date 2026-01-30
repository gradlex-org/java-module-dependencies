// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES;
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES_RUNTIME;
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES_STATIC;
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES_STATIC_TRANSITIVE;
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES_TRANSITIVE;

import org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo;
import org.junit.jupiter.api.Test;

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
