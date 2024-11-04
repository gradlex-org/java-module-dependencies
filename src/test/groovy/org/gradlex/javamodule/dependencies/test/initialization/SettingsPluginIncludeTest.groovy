package org.gradlex.javamodule.dependencies.test.initialization

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class SettingsPluginIncludeTest extends Specification {

    @Delegate
    GradleBuild build = new GradleBuild()

    def setup() {
        settingsFile.text = '''
            plugins { id("org.gradlex.java-module-dependencies") }
        '''
        appBuildFile.delete()
        libBuildFile.delete()
    }

    def "can define included subprojects as modules"() {
        given:
        settingsFile << '''
            include(":project:with:custom:path")
            javaModules { 
                module(project(":project:with:custom:path")) {
                    group = "org.example"
                    plugin("java-library")
                }
                module(project(":project:with:custom")) {
                    group = "org.example"
                    plugin("java-library")
                }
            }
        '''

        file('project/with/custom/path/src/main/java/module-info.java') << 'module abc.liba { }'
        file('project/with/custom/src/main/java/module-info.java') << '''module abc.libb {
            requires abc.liba;
        }'''

        when:
        def result = runner(':project:with:custom:compileJava').build()

        then:
        result.task(":project:with:custom:path:compileJava").outcome == SUCCESS
        result.task(":project:with:custom:compileJava").outcome == SUCCESS
    }

    def "can define included subprojects with custom project directory as modules"() {
        given:
        settingsFile << '''
            include(":project:with:custom:path")
            project(":project:with:custom:path").projectDir = file("lib")
            project(":project:with:custom").projectDir = file("app")
            javaModules { 
                module(project(":project:with:custom:path")) {
                    group = "org.example"
                    plugin("java-library")
                }
                module(project(":project:with:custom")) {
                    group = "org.example"
                    plugin("java-library")
                }
            }
        '''

        file("project/with").mkdirs()
        libModuleInfoFile << 'module abc.lib { }'
        appModuleInfoFile << '''module abc.app {
            requires abc.lib;
        }'''

        when:
        def result = runner(':project:with:custom:jar').build()

        then:
        result.task(":project:with:custom:path:compileJava").outcome == SUCCESS
        result.task(":project:with:custom:compileJava").outcome == SUCCESS
        file("lib/build/libs/path.jar").exists()
        file("app/build/libs/custom.jar").exists()
    }

    def "projects with same name but different paths are supported"() {
        given:
        settingsFile << '''
            include(":app1:feature1:data")
            include(":app1:feature2:data")
            
            rootProject.children.forEach { appContainer ->
                appContainer.children.forEach { featureContainer ->
                    featureContainer.children.forEach { module ->
                        javaModules.module(module) { plugin("java-library") }
                    }
                }
            }
        '''

        file('app1/feature1/data/src/main/java/module-info.java') << 'module f1x.data { }'
        file('app1/feature2/data/src/main/java/module-info.java') << '''module f2x.data {
            requires f1x.data;
        }'''

        when:
        def result = runner(':app1:feature2:data:jar').build()

        then:
        result.task(":app1:feature1:data:jar").outcome == SUCCESS
        result.task(":app1:feature2:data:jar").outcome == SUCCESS
        file("app1/feature1/data/build/libs/data.jar").exists()
        file("app1/feature2/data/build/libs/data.jar").exists()
    }
}
