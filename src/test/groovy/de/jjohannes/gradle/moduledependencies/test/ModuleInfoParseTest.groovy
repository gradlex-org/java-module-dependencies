package de.jjohannes.gradle.moduledependencies.test

import de.jjohannes.gradle.moduledependencies.internal.utils.ModuleInfo
import spock.lang.Specification

import static de.jjohannes.gradle.moduledependencies.internal.utils.ModuleInfo.Directive.REQUIRES
import static de.jjohannes.gradle.moduledependencies.internal.utils.ModuleInfo.Directive.REQUIRES_TRANSITIVE
import static de.jjohannes.gradle.moduledependencies.internal.utils.ModuleInfo.Directive.REQUIRES_STATIC
import static de.jjohannes.gradle.moduledependencies.internal.utils.ModuleInfo.Directive.REQUIRES_STATIC_TRANSITIVE

class ModuleInfoParseTest extends Specification {

    def "ignores single line comments"() {
        given:
        def moduleInfo = new ModuleInfo('''
            module some.thing {
                // requires com.bla.blub;
                requires transitive foo.bar.la;
            }
        ''')

        expect:
        moduleInfo.get(REQUIRES) == []
        moduleInfo.get(REQUIRES_TRANSITIVE) == ["foo.bar.la"]
        moduleInfo.get(REQUIRES_STATIC) == []
        moduleInfo.get(REQUIRES_STATIC_TRANSITIVE) == []
    }

    def "ignores multi line comments"() {
        given:
        def moduleInfo = new ModuleInfo('''
            module some.thing {
                /* requires com.bla.blub;
                requires transitive foo.bar.la;
                */
                requires static foo.bar.la;
            }
        ''')

        expect:
        moduleInfo.get(REQUIRES) == []
        moduleInfo.get(REQUIRES_TRANSITIVE) == []
        moduleInfo.get(REQUIRES_STATIC) == ["foo.bar.la"]
        moduleInfo.get(REQUIRES_STATIC_TRANSITIVE) == []
    }

}
