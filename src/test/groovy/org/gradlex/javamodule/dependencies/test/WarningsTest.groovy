package org.gradlex.javamodule.dependencies.test

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild
import spock.lang.Specification

class WarningsTest extends Specification {

    @Delegate
    GradleBuild build = new GradleBuild()

    def "warning for version missing in catalog can be disabled"() {
        given:
        file('gradle/libs.versions.toml') << '''
            [versions]
            org_apache_xmlbeans = "5.0.1"
        '''.stripIndent()
        appBuildFile << '''
            javaModuleDependencies.warnForMissingVersions.set(false)
            dependencies.constraints {
                javaModuleDependencies {
                    implementation(gav("com.fasterxml.jackson.databind", "2.12.5"))
                }
            }
        '''
        appModuleInfoFile << '''
            module org.gradlex.test.app { 
                requires com.fasterxml.jackson.databind;
                requires org.apache.xmlbeans;
            }
        '''

        when:
        def result = build()

        then:
        !result.output.contains('[WARN]')
    }

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
