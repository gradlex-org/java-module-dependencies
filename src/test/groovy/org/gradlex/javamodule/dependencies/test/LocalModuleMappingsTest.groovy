package org.gradlex.javamodule.dependencies.test

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild
import spock.lang.Specification

class LocalModuleMappingsTest extends Specification {

    @Delegate
    GradleBuild build = new GradleBuild()

    def "automatically maps local modules if name prefix matches"() {
        when:
        libModuleInfoFile << 'module org.gradlex.test.lib { }'
        appModuleInfoFile << '''
            module org.gradlex.test.app {
                requires org.gradlex.test.lib;
            }
        '''

        then:
        build()
    }

    def "automatically maps local modules if name matches"() {
        when:
        libModuleInfoFile << 'module lib { }'
        appModuleInfoFile << '''
            module app {
                requires lib;
            }
        '''

        then:
        build()
    }

    def "a prefix-to-group mapping can be used"() {
        when:
        appBuildFile << '''
            javaModuleDependencies.moduleNamePrefixToGroup.put("abc.", "foo.gr")
        '''
        libBuildFile << '''
            group = "foo.gr"
        '''

        libModuleInfoFile << 'module abc.lib { }'
        appModuleInfoFile << '''
            module org.gradlex.test.app {
                requires abc.lib;
            }
        '''

        then:
        runner(false, 'build').build()
    }
}
