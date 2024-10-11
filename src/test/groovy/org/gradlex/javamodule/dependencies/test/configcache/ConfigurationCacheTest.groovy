package org.gradlex.javamodule.dependencies.test.configcache

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild
import spock.lang.Specification

import static org.gradle.util.GradleVersion.version

class ConfigurationCacheTest extends Specification {

    @Delegate
    GradleBuild build = new GradleBuild()

    final noCacheMessage = version(gradleVersionUnderTest) >= version("8.8")
            ? "Calculating task graph as no cached configuration is available for tasks: :app:compileJava"
            : "Calculating task graph as no configuration cache is available for tasks: :app:compileJava"

    def "configurationCacheHit"() {
        given:
        libModuleInfoFile << 'module abc.lib { }'

        appModuleInfoFile << '''
            module abc.app {
                requires abc.lib;
            }
        '''

        def runner = runner('--configuration-cache',':app:compileJava')
        when:
        def result = runner.build()

        then:
        result.output.contains(noCacheMessage)

        when:
        result = runner.build()

        then:
        result.output.contains("Reusing configuration cache.")
    }

    def "configurationCacheHitIrrelevantChange"() {
        libModuleInfoFile << 'module abc.lib { }'
        appModuleInfoFile << '''
            module abc.app {
                requires abc.lib;
            }
        '''

        def runner = runner('--configuration-cache',':app:compileJava')
        when:
        def result = runner.build()

        then:
        result.output.contains(noCacheMessage)

        when:
        appModuleInfoFile.write('''
            module abc.app {
                requires abc.lib; //This is a comment and should not break the configurationCache
            }
        ''')
        result = runner.build()

        then:
        result.output.contains("Reusing configuration cache.")
    }

    def "configurationCacheMissRelevantChange"() {
        given:
        libModuleInfoFile << 'module abc.lib { }'
        appModuleInfoFile << '''
            module abc.app {
                requires abc.lib;
            }
        '''

        def runner = runner('--configuration-cache',':app:compileJava')
        when:
        def result = runner.build()

        then:
        result.output.contains(noCacheMessage)

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
