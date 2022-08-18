package org.gradlex.javamodule.dependencies.test

import org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo
import spock.lang.Specification

import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES_TRANSITIVE
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES_STATIC
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES_STATIC_TRANSITIVE

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
        moduleInfo.moduleNamePrefix("thing", "main") == "some"
        moduleInfo.get(REQUIRES) == []
        moduleInfo.get(REQUIRES_TRANSITIVE) == ["foo.bar.la"]
        moduleInfo.get(REQUIRES_STATIC) == []
        moduleInfo.get(REQUIRES_STATIC_TRANSITIVE) == []
    }

    def "ignores single line comments late in line"() {
        given:
        def moduleInfo = new ModuleInfo('''
            module some.thing { // module some.thing.else
                requires transitive foo.bar.la;
            }
        ''')

        expect:
        moduleInfo.moduleNamePrefix("thing", "main") == "some"
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
