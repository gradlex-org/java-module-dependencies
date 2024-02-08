package org.gradlex.javamodule.dependencies.test

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild
import spock.lang.Specification

class BasicFunctionalityTest extends Specification {

    @Delegate
    GradleBuild build = new GradleBuild()

    def "can configure all tasks in a build without error"() {
        given:
        libModuleInfoFile << 'module abc.lib { }'
        appModuleInfoFile << '''
            module org.gradlex.test.app {
                requires abc.lib;
            }
        '''

        when:
        def result = runner('tasks').build()

        then:
        result.output.contains('''
            Java modules tasks
            ------------------
            checkModuleInfo - Check order of directives in 'module-info.java' in 'main' source set
            checkTestFixturesModuleInfo - Check order of directives in 'module-info.java' in 'testFixtures' source set
            checkTestModuleInfo - Check order of directives in 'module-info.java' in 'test' source set
            generateAllModuleInfoFiles - Generate 'module-info.java' files in all source sets
            generateBuildFileDependencies - Generate 'dependencies' block in 'build.gradle.kts'
            generateCatalog - Generate 'libs.versions.toml' file
            generateModuleInfoFile - Generate 'module-info.java' in 'main' source set
            generateTestFixturesModuleInfoFile - Generate 'module-info.java' in 'testFixtures' source set
            generateTestModuleInfoFile - Generate 'module-info.java' in 'test' source set'''.stripIndent()
        )
    }

}
