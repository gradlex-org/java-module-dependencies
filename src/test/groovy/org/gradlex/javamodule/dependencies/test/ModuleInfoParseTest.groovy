package org.gradlex.javamodule.dependencies.test

import org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo
import spock.lang.Specification

import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES_RUNTIME
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
        moduleInfo.moduleNamePrefix("thing", "main", false) == "some"
        moduleInfo.get(REQUIRES) == []
        moduleInfo.get(REQUIRES_TRANSITIVE) == ["foo.bar.la"]
        moduleInfo.get(REQUIRES_STATIC) == []
        moduleInfo.get(REQUIRES_STATIC_TRANSITIVE) == []
        moduleInfo.get(REQUIRES_RUNTIME) == []
    }

    def "ignores single line comments late in line"() {
        given:
        def moduleInfo = new ModuleInfo('''
            module some.thing { // module some.thing.else
                requires transitive foo.bar.la;
            }
        ''')

        expect:
        moduleInfo.moduleNamePrefix("thing", "main", false) == "some"
        moduleInfo.get(REQUIRES) == []
        moduleInfo.get(REQUIRES_TRANSITIVE) == ["foo.bar.la"]
        moduleInfo.get(REQUIRES_STATIC) == []
        moduleInfo.get(REQUIRES_STATIC_TRANSITIVE) == []
        moduleInfo.get(REQUIRES_RUNTIME) == []
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
        moduleInfo.get(REQUIRES_RUNTIME) == []
    }

    def "ignores multi line comments between keywords"() {
        given:
        def moduleInfo = new ModuleInfo('''
            module some.thing {
                /*odd comment*/ requires transitive foo.bar.la;
                requires/* weird comment*/ static foo.bar.lo;
                requires /*something to say*/foo.bar.li; /*
                    requires only.a.comment
                */
            }
        ''')

        expect:
        moduleInfo.get(REQUIRES) == ["foo.bar.li"]
        moduleInfo.get(REQUIRES_TRANSITIVE) == ["foo.bar.la"]
        moduleInfo.get(REQUIRES_STATIC) == ["foo.bar.lo"]
        moduleInfo.get(REQUIRES_STATIC_TRANSITIVE) == []
        moduleInfo.get(REQUIRES_RUNTIME) == []
    }

    def "supports runtime dependencies through special keyword"() {
        given:
        def moduleInfo = new ModuleInfo('''
            module some.thing {
                requires /*runtime*/ foo.bar.lo;
            }
        ''')

        expect:
        moduleInfo.get(REQUIRES) == []
        moduleInfo.get(REQUIRES_TRANSITIVE) == []
        moduleInfo.get(REQUIRES_STATIC) == []
        moduleInfo.get(REQUIRES_STATIC_TRANSITIVE) == []
        moduleInfo.get(REQUIRES_RUNTIME) == ["foo.bar.lo"]
    }

}
