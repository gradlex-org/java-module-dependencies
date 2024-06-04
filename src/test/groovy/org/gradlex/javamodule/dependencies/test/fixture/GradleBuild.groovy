package org.gradlex.javamodule.dependencies.test.fixture

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

import java.lang.management.ManagementFactory
import java.nio.file.Files

class GradleBuild {

    final File projectDir
    final File settingsFile
    final File appBuildFile
    final File libBuildFile
    final File appModuleInfoFile
    final File libModuleInfoFile

    final String gradleVersionUnderTest = System.getProperty("gradleVersionUnderTest")

    GradleBuild(File projectDir = Files.createTempDirectory("gradle-build").toFile()) {
        this.projectDir = projectDir
        this.settingsFile = file("settings.gradle.kts")
        this.appBuildFile = file("app/build.gradle.kts")
        this.libBuildFile = file("lib/build.gradle.kts")
        this.appModuleInfoFile = file("app/src/main/java/module-info.java")
        this.libModuleInfoFile = file("lib/src/main/java/module-info.java")

        settingsFile << '''
            dependencyResolutionManagement { repositories.mavenCentral() }
            rootProject.name = "test-project"
            include("lib", "app")
            includeBuild(".")
        '''
        appBuildFile << '''
            plugins {
                id("org.gradlex.java-module-dependencies")
                id("org.gradlex.java-module-versions")
                id("application")
            }
            javaModuleDependencies {
                versionsFromPlatformAndConsistentResolution(":app", ":app")
            }
            application {
                mainModule.set("org.gradlex.test.app")
                mainClass.set("org.gradlex.test.app.Main")
            }
            tasks.register("printRuntimeJars") {
                inputs.files(configurations.runtimeClasspath)
                doLast { println(inputs.files.map { it.name }) }
            }
            tasks.register("printCompileJars") {
                inputs.files(configurations.compileClasspath)
                doLast { println(inputs.files.map { it.name }) }
            }
        '''
        libBuildFile << '''
            plugins {
                id("org.gradlex.java-module-dependencies")
                id("java-library")
                id("java-test-fixtures")
            }
        '''
    }

    File file(String path) {
        new File(projectDir, path).tap {
            it.getParentFile().mkdirs()
        }
    }

    BuildResult build() {
        runner('build').build()
    }

    BuildResult run() {
        runner('run').build()
    }

    BuildResult printRuntimeJars() {
        runner(':app:printRuntimeJars', '-q').build()
    }
    BuildResult printCompileJars() {
        runner(':app:printCompileJars', '-q').build()
    }

    BuildResult fail() {
        runner('build').buildAndFail()
    }

    GradleRunner runner(boolean projectIsolation = true, String... args) {
        List<String> latestFeaturesArgs = gradleVersionUnderTest || !projectIsolation? [] : [
                '--configuration-cache',
                '-Dorg.gradle.unsafe.isolated-projects=true',
                // 'getGroup' in 'JavaModuleDependenciesExtension.create'
                '--configuration-cache-problems=warn', '-Dorg.gradle.configuration-cache.max-problems=3'
        ]
        GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(projectDir)
                .withArguments(Arrays.asList(args) + latestFeaturesArgs + '-s' + '--warning-mode=all')
                .withDebug(ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("-agentlib:jdwp")).with {
            gradleVersionUnderTest ? it.withGradleVersion(gradleVersionUnderTest) : it
        }
    }
}
