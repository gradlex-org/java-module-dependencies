package org.gradlex.javamodule.dependencies.test

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild
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
            module org.gradlex.test.app { 
                requires jakarta.mail;
            }
        '''

        when:
        def result = printRuntimeJars()

        then:
        result.output.contains('[jakarta.mail-2.0.1.jar, jakarta.activation-2.0.1.jar]')
    }

    def "can add custom mapping via properties file (default location)"() {
        given:
        def customModulesPropertiesFile = file("gradle/modules.properties")

        customModulesPropertiesFile << 'jakarta.mail=com.sun.mail:jakarta.mail'
        appBuildFile << 'moduleInfo { version("jakarta.mail", "2.0.1") }'

        appModuleInfoFile << '''
            module org.gradlex.test.app { 
                requires jakarta.mail;
            }
        '''

        when:
        def result = printRuntimeJars()

        then:
        result.output.contains('[jakarta.mail-2.0.1.jar, jakarta.activation-2.0.1.jar]')
    }

    def "can add custom mapping via properties file (custom location)"() {
        given:
        def customModulesPropertiesFile = file(".hidden/modules.properties")

        customModulesPropertiesFile << 'jakarta.mail=com.sun.mail:jakarta.mail'
        appBuildFile << 'moduleInfo { version("jakarta.mail", "2.0.1") }'

        appBuildFile << '''
            javaModuleDependencies {
                modulesProperties.set(File(rootDir,".hidden/modules.properties"))
            }
        '''

        appModuleInfoFile << '''
            module org.gradlex.test.app { 
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
                version("org.apache.xmlbeans", "5.0.1")
                version("com.fasterxml.jackson.databind", "2.12.5")
            }
        '''
        appBuildFile << '''
            javaModuleDependencies.versionCatalogName.set("modules")
        '''
        appModuleInfoFile << '''
            module org.gradlex.test.app { 
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

    def "can define versions in platform with different notations"() {
        given:
        def customModulesPropertiesFile = file("gradle/modules.properties")

        customModulesPropertiesFile << 'jakarta.mail=com.sun.mail:jakarta.mail'
        appBuildFile << '''
            moduleInfo { 
                version("jakarta.mail", "2.0.1")
                version("jakarta.servlet", "6.0.0") { reject("[7.0.0,)") }
                version("java.inject") { require("1.0.5"); reject("[2.0.0,)") }
            }
        '''

        appModuleInfoFile << '''
            module org.gradlex.test.app { 
                requires jakarta.mail;
                requires jakarta.servlet;
                requires java.inject;
            }
        '''

        when:
        def result = printRuntimeJars()

        then:
        result.output.contains('[jakarta.mail-2.0.1.jar, jakarta.servlet-api-6.0.0.jar, jakarta.inject-api-1.0.5.jar, jakarta.activation-2.0.1.jar]')
    }

    def "can use toml catalog with '_' for '.'"() {
        given:
        file('gradle/libs.versions.toml') << '''
            [versions]
            org_apache_xmlbeans = "5.0.1"
        '''.stripIndent()
        appModuleInfoFile << '''
            module org.gradlex.test.app { 
                requires org.apache.xmlbeans;
            }
        '''

        when:
        def result = build()

        then:
        !result.output.contains('[WARN]')
    }

    def "can use toml catalog with '-' for '.'"() {
        given:
        file('gradle/libs.versions.toml') << '''
            [versions]
            org-apache-xmlbeans = "5.0.1"
        '''.stripIndent()
        appModuleInfoFile << '''
            module org.gradlex.test.app { 
                requires org.apache.xmlbeans;
            }
        '''

        when:
        def result = build()

        then:
        !result.output.contains('[WARN]')
    }
}
