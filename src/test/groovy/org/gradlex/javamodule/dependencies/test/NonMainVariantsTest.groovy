package org.gradlex.javamodule.dependencies.test

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild
import spock.lang.Specification

class NonMainVariantsTest extends Specification {

    @Delegate
    GradleBuild build = new GradleBuild()

    def "finds test fixtures module"() {
        given:
        appModuleInfoFile << '''
            module org.gradlex.test.app { 
                requires org.gradlex.test.lib.test.fixtures;
            }
        '''
        libModuleInfoFile << '''
            module org.gradlex.test.lib {
            }
        '''
        file("lib/src/testFixtures/java/module-info.java") << '''
            module org.gradlex.test.lib.test.fixtures {
            }
        '''

        when:
        def result = printRuntimeJars()

        then:
        result.output.contains('[lib-test-fixtures.jar, lib.jar]')
    }

    def "finds feature variant module"() {
        given:
        libBuildFile << '''
            val extraFeature = sourceSets.create("extraFeature")
            java.registerFeature(extraFeature.name) {
                usingSourceSet(extraFeature)
            }
        '''
        
        appModuleInfoFile << '''
            module org.gradlex.test.app { 
                requires org.gradlex.test.lib.extra.feature;
            }
        '''
        libModuleInfoFile << '''
            module org.gradlex.test.lib {
            }
        '''
        file("lib/src/extraFeature/java/module-info.java") << '''
            module org.gradlex.test.lib.extra.feature {
            }
        '''

        when:
        def result = printRuntimeJars()

        then:
        result.output.contains('[lib-extra-feature.jar')
    }
}
