package org.gradlex.javamodule.dependencies.test

import org.gradle.testkit.runner.TaskOutcome
import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild
import spock.lang.Specification

class GroupMappingTest extends Specification {

    @Delegate
    GradleBuild build = new GradleBuild()

    def "can map overlapping groups"() {
        given:
        def lib2ModuleInfoFile = file("lib-b/src/main/java/module-info.java")
        def lib2BuildFile = file("lib-b/build.gradle.kts") << libBuildFile.text
        settingsFile << 'include("lib-b")'

        libModuleInfoFile << 'module com.lib { }'
        libBuildFile << 'group = "com.foo"'
        lib2ModuleInfoFile << 'module com.example.lib.b { }'
        lib2BuildFile << 'group = "com.example"'
        appModuleInfoFile << '''
            module org.gradlex.test.app {
                requires com.lib;
                requires com.example.lib.b;
            }
        '''

        appBuildFile << '''
            javaModuleDependencies {
                moduleNamePrefixToGroup.put("com.", "com.foo")
                moduleNamePrefixToGroup.put("com.example.", "com.example")
            }
        '''

        when:
        def result = runner(false, 'assemble').build()

        then:
        result.task(":app:compileJava").outcome == TaskOutcome.SUCCESS
    }

}
