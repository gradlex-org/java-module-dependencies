package de.jjohannes.gradle.moduledependencies.test

import de.jjohannes.gradle.moduledependencies.test.fixture.GradleBuild
import spock.lang.Specification

class CustomizationTest extends Specification {

    @Delegate
    GradleBuild build = new GradleBuild()

    def "can add custom mapping"() {
        given:
        appBuildFile << '''
            javaModuleDependencies {
                // Override because there are multiple alternatives
                moduleNameToGA.put("jakarta.mail", "com.sun.mail:jakarta.mail")
            }
            dependencies.constraints {
                javaModuleDependencies {
                    implementation(gav("jakarta.mail", "2.0.1"))
                }
            }
        '''
        appModuleInfoFile << '''
            module de.jjohannes.test.app { 
                requires jakarta.mail;
            }
        '''

        when:
        def result = printRuntimeJars()

        then:
        result.output.contains('[jakarta.mail-2.0.1.jar, jakarta.activation-2.0.1.jar]')
    }

    def "can use custom catalog"() {
        given:
        settingsFile << '''
            dependencyResolutionManagement.versionCatalogs.create("modules") {
                version("org_apache_xmlbeans", "5.0.1")
                version("com_fasterxml_jackson_databind", "2.12.5")
            }
        '''
        appBuildFile << '''
            javaModuleDependencies.versionCatalogName.set("modules")
        '''
        appModuleInfoFile << '''
            module de.jjohannes.test.app { 
                requires com.fasterxml.jackson.databind;
                requires static org.apache.xmlbeans;
            }
        '''

        when:
        def runtime = printRuntimeJars()
        then:
        runtime.output.contains('[jackson-annotations-2.12.5.jar, jackson-core-2.12.5.jar, jackson-databind-2.12.5.jar]')

        when:
        def compile = printCompileJars()
        then:
        compile.output.contains('[xmlbeans-5.0.1.jar, jackson-annotations-2.12.5.jar, jackson-core-2.12.5.jar, jackson-databind-2.12.5.jar, log4j-api-2.14.0.jar]')
    }
}
