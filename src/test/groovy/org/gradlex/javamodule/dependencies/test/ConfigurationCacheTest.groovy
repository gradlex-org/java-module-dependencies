package org.gradlex.javamodule.dependencies.test

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild
import spock.lang.Specification

class ConfigurationCacheTest extends Specification {

    @Delegate
    GradleBuild build = new GradleBuild()

    def "configurationCacheHit"() {
        given:
        libModuleInfoFile << 'module abc.lib { }'
        appModuleInfoFile << '''
            module abc.app {
                requires abc.lib;
            }
        '''


        def runner = runner(':app:compileJava')
        when:
        def result = runner.build()

        then:
        result.getOutput().contains("Calculating task graph as no cached configuration is available for tasks: :app:compileJava")

        when:
        result = runner.build()

        then:
        result.getOutput().contains("Reusing configuration cache.")
    }

    def "configurationCacheHitIrrelevantChange"() {
        libModuleInfoFile << 'module abc.lib { }'
        appModuleInfoFile << '''
            module abc.app {
                requires abc.lib;
            }
        '''

        def runner = runner(':app:compileJava')
        when:
        def result = runner.build()

        then:
        result.getOutput().contains("Calculating task graph as no cached configuration is available for tasks: :app:compileJava")

        when:
        appModuleInfoFile.write('''
            module abc.app {
                requires abc.lib; //This is a comment and should not break the configurationCache
            }
        ''')
        result = runner.build()

        then:
        result.getOutput().contains("Reusing configuration cache.")
    }

    def "configurationCacheHitRelevantChange"() {
        given:
        libModuleInfoFile << 'module abc.lib { }'
        appModuleInfoFile << '''
            module abc.app {
                requires abc.lib;
            }
        '''

        def runner = runner(':app:compileJava')
        when:
        def result = runner.build()

        then:
        result.getOutput().contains("Calculating task graph as no cached configuration is available for tasks: :app:compileJava")

        when:
        appModuleInfoFile.write('''
            module abc.app {
               //dependency removed; so thats indeed a configuration change
            }
        ''')
        result = runner.build()

        then:
        result.output.contains("Calculating task graph as configuration cache cannot be reused because a build logic input of type 'ValueSourceModuleInfo' has changed.\n")
    }

}
