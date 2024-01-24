package org.gradlex.javamodule.dependencies.test

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild
import spock.lang.Specification

class WarningsTest extends Specification {

    @Delegate
    GradleBuild build = new GradleBuild()

    def "prints warning for missing mapping"() {
        given:
        appModuleInfoFile << '''
            module org.my.app {
                requires commons.math3;
            }
        '''

        when:
        def result = fail()
        then:
        result.output.contains('[WARN] [Java Module Dependencies] commons.math3=group:artifact missing in')
    }
}
