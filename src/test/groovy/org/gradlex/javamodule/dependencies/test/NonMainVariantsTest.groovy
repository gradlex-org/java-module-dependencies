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

    def "finds published feature variant when corresponding mapping is defined"() {
        // There are no modules published like this anywhere public right now.
        // We test that the expected Jar file would have been downloaded if 'org.slf4j' would have test fixtures.
        given:
        appBuildFile << '''
            javaModuleDependencies {
                moduleNameToGA.put("org.slf4j.test.fixtures", "org.slf4j:slf4j-api|org.slf4j:slf4j-api-test-fixtures")
            }
            dependencies.constraints {
                javaModuleDependencies { implementation(gav("org.slf4j", "2.0.3")) }
            }
        '''
        appModuleInfoFile << '''
            module org.gradlex.test.app { 
                requires org.slf4j.test.fixtures;
            }
        '''

        when:
        def result = fail()

        then:
        result.output.contains('> Unable to find a variant of org.slf4j:slf4j-api:2.0.3 providing the requested capability org.slf4j:slf4j-api-test-fixtures')
    }
}
