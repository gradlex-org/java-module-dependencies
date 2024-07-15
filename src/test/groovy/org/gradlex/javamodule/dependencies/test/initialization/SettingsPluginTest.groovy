package org.gradlex.javamodule.dependencies.test.initialization

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class SettingsPluginTest extends Specification {

    @Delegate
    GradleBuild build = new GradleBuild()

    def setup() {
        settingsFile.text = '''
            plugins { id("org.gradlex.java-module-dependencies") }
        '''
        appBuildFile.delete()
        libBuildFile.delete()
    }

    def "can define individual modules"() {
        given:
        settingsFile << '''
            javaModules {
                module("app") { plugin("application") }
                module("lib") { plugin("java-library") }
            }
        '''
        libModuleInfoFile << 'module abc.lib { }'
        appModuleInfoFile << '''
            module org.gradlex.test.app {
                requires abc.lib;
            }
        '''

        when:
        def result = runner(':app:compileJava').build()

        then:
        result.task(":app:compileJava").outcome == SUCCESS
        result.task(":lib:compileJava").outcome == SUCCESS
    }

    def "finds all modules in a directory"() {
        given:
        settingsFile << '''
            javaModules {
                directory(".") { plugin("java-library") }
            }
        '''
        libModuleInfoFile << 'module abc.lib { }'
        appModuleInfoFile << '''
            module org.gradlex.test.app {
                requires abc.lib;
            }
        '''

        when:
        def result = runner(':app:build').build()

        then:
        result.task(":app:compileJava").outcome == SUCCESS
        result.task(":lib:compileJava").outcome == SUCCESS
    }

    def "automatically sets module for application plugin"() {
        given:
        settingsFile << '''
            javaModules {
                directory(".") {
                    plugin("java-library") 
                    module("app") { plugin("application") }
                }
            }
        '''
        libModuleInfoFile << 'module abc.libxyz { }'
        appModuleInfoFile << '''
            module org.gradlex.test.app {
                requires abc.libxyz;
            }
        '''
        appBuildFile << 'application.mainClass = "app.App"'
        file("app/src/main/java/app/App.java") << 'package app; public class App { public static void main(String[] args) { } }'

        when:
        def result = runner(':app:run').build()

        then:
        result.task(":app:run").outcome == SUCCESS
        result.task(":app:compileJava").outcome == SUCCESS
        result.task(":lib:compileJava").outcome == SUCCESS
    }

    def "can depend on test fixtures module"() {
        given:
        settingsFile << '''
            javaModules {
                directory(".") {
                    group = "bar.foo"
                    plugin("java-library") 
                    plugin("java-test-fixtures") 
                }
            }
        '''
        libModuleInfoFile << 'module foo.bar.m { }'
        file('lib/src/testFixtures/java/module-info.java') << 'module abc.libxyz.dsdsds { }'
        appModuleInfoFile << '''
            module org.gradlex.test.app {
                requires foo.bar.m;
                requires abc.libxyz.dsdsds;
            }
        '''

        when:
        def result = runner(':app:compileJava').build()

        then:
        result.task(":app:compileJava").outcome == SUCCESS
        result.task(":lib:compileJava").outcome == SUCCESS
        result.task(":lib:compileTestFixturesJava").outcome == SUCCESS
    }

    def 'can apply root project plugin from settings'() {
        settingsFile << '''
            rootPlugins {
                id("java")
            }
        '''

        when:
        def result = runner(':compileJava').build()

        then:
        result.task(":compileJava").outcome == NO_SOURCE
    }

}
