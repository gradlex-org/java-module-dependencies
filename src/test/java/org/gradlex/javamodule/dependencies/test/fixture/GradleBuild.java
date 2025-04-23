/*
 * Copyright the GradleX team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradlex.javamodule.dependencies.test.fixture;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Function.identity;

public class GradleBuild {

    private final boolean withHelpTasks;

    public final Directory projectDir;
    public final WritableFile settingsFile;
    public final WritableFile appBuildFile;
    public final WritableFile libBuildFile;
    public final WritableFile appModuleInfoFile;
    public final WritableFile libModuleInfoFile;

    public static final String GRADLE_VERSION_UNDER_TEST = System.getProperty("gradleVersionUnderTest");

    public GradleBuild() {
        this(false, createBuildTmpDir());
    }

    public GradleBuild(boolean withHelpTasks) {
        this(withHelpTasks, createBuildTmpDir());
    }

    public GradleBuild(boolean withHelpTasks, Path dir) {
        this.withHelpTasks = withHelpTasks;
        this.projectDir = new Directory(dir);
        this.settingsFile = new WritableFile(projectDir, "settings.gradle.kts");
        this.appBuildFile = new WritableFile(projectDir.dir("app"), "build.gradle.kts");
        this.libBuildFile = new WritableFile(projectDir.dir("lib"), "build.gradle.kts");
        this.appModuleInfoFile = new WritableFile(projectDir.dir("app/src/main/java"), "module-info.java");
        this.libModuleInfoFile = new WritableFile(projectDir.dir("lib/src/main/java"), "module-info.java");

        settingsFile.writeText("""
            dependencyResolutionManagement { repositories.mavenCentral() }
            rootProject.name = "test-project"
            include("lib", "app")
            includeBuild(".")
        """);
        appBuildFile.writeText("""
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
        """);
        libBuildFile.writeText("""   
                    plugins {
                        id("org.gradlex.java-module-dependencies")
                        id("java-library")
                        id("java-test-fixtures")
                    }
                """);
    }

    public WritableFile file(String path) {
        return new WritableFile(projectDir, path);
    }

    public BuildResult build() {
        return runner("build").build();
    }

    public BuildResult run() {
        return runner("run").build();
    }

    public BuildResult printRuntimeJars() {
        return runner(":app:printRuntimeJars", "-q").build();
    }
    public BuildResult printCompileJars() {
        return runner(":app:printCompileJars", "-q").build();
    }

    public BuildResult fail() {
        return runner("build").buildAndFail();
    }

    public GradleRunner runner(String... args) {
        return runner(true, args);
    }

    public GradleRunner runner(boolean projectIsolation, String... args) {
        boolean debugMode = ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("-agentlib:jdwp");
        List<String> latestFeaturesArgs = GRADLE_VERSION_UNDER_TEST != null || !projectIsolation ? List.of() : List.of(
                "--configuration-cache",
                "-Dorg.gradle.unsafe.isolated-projects=true",
                // "getGroup" in "JavaModuleDependenciesExtension.create"
                "--configuration-cache-problems=warn", "-Dorg.gradle.configuration-cache.max-problems=3"
        );
        Stream<String> standardArgs = Stream.of(
                "-s",
                "--warning-mode=all",
                "-Porg.gradlex.java-module-dependencies.register-help-tasks=" + withHelpTasks
        );
        GradleRunner runner = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withDebug(debugMode)
                .withProjectDir(projectDir.getAsPath().toFile())
                .withArguments(Stream.of(Arrays.stream(args), latestFeaturesArgs.stream(), standardArgs)
                        .flatMap(identity()).collect(Collectors.toList()));
        if (GRADLE_VERSION_UNDER_TEST != null) {
            runner.withGradleVersion(GRADLE_VERSION_UNDER_TEST);
        }
        return runner;
    }

    private static Path createBuildTmpDir() {
        try {
            return Files.createTempDirectory("gradle-build");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
