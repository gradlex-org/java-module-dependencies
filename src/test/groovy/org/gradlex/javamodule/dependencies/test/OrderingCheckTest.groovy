package org.gradlex.javamodule.dependencies.test

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild
import spock.lang.Specification

class OrderingCheckTest extends Specification {

    @Delegate
    GradleBuild build = new GradleBuild()

    def "order is expected to be alphabetic for each scope individually"() {
        when:
        appModuleInfoFile << '''
            module org.example.app {
                requires a.b.c
                requires b.f.g
                requires b.z.u
                
                requires static c.w.q
                requires static c.z.u
            }
        '''

        then:
        runner(":app:checkAllModuleInfo").build()
    }

    def "if order is not alphabetic for a scope, an advice is given"() {
        when:
        appModuleInfoFile << '''
            module org.example.app {
                requires a.b.c
                requires b.z.u
                requires b.f.g
                
                requires static c.w.q
                requires static c.z.u
            }
        '''

        then:
        def result = runner(":app:checkAllModuleInfo").buildAndFail()
        result.output.contains('''
            |> app/src/main/java/module-info.java
            |  
            |  'requires' are not declared in alphabetical order. Please use this order:
            |      requires a.b.c;
            |      requires b.f.g;
            |      requires b.z.u;'''.stripMargin()
        )
    }

    def "own modules are expected to go first"() {
        when:
        appModuleInfoFile << '''
            module org.example.app {
                requires org.example.h
                requires org.example.j
                
                requires a.b.c
                requires b.f.g
                requires z.z.u
                
                requires static org.example.z
                requires static c.w.q
                requires static c.z.u
            }
        '''

        then:
        runner(":app:checkAllModuleInfo").build()
    }

}