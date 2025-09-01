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

package org.gradlex.javamodule.dependencies.test.runtime;

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

class RequiresRuntimeTest {

    GradleBuild build = new GradleBuild();

    @Test
    void can_define_runtime_only_dependencies_in_moduleinfo() {
        build.appBuildFile.appendText("""
            dependencies.constraints {
                javaModuleDependencies {
                    implementation(gav("org.slf4j", "2.0.3"))
                    implementation(gav("org.slf4j.simple", "2.0.3"))
                }
            }""");
        build.appModuleInfoFile.appendText("""
            module org.gradlex.test.app {
                requires org.slf4j;
                requires /*runtime*/ org.slf4j.simple;
            }""");

        var rt = build.printRuntimeJars();

        assertThat(rt.getOutput()).contains("[slf4j-simple-2.0.3.jar, slf4j-api-2.0.3.jar]");

        var cp = build.printCompileJars();

        assertThat(cp.getOutput()).contains("[org.slf4j.simple, slf4j-api-2.0.3.jar]"); // 'org.slf4j.simple' is the folder nam of synthetic 'module-info.class'
    }

    @Test
    void compiles_with_runtime_only_dependencies_in_moduleinfo() {
        build.appBuildFile.appendText("""
            dependencies.constraints {
                javaModuleDependencies {
                    implementation(gav("org.slf4j", "2.0.3"))
                    implementation(gav("org.slf4j.simple", "2.0.3"))
                }
            }""");
        build.appModuleInfoFile.writeText("""
            module org.gradlex.test.app {
                requires org.slf4j;
                requires /*runtime*/ org.slf4j.simple;
            
                exports org.gradlex.test.app;
            }""");
        build.file("app/src/main/java/org/gradlex/test/app/Main.java").writeText("""
            package org.gradlex.test.app;
            
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            
            public class Main {
                private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
            
                public static void main(String[] args) {
                    LOGGER.info("Running application...");
                }
            }""");

        var result = build.run();

        assertThat(result.getOutput()).contains("[main] INFO org.gradlex.test.app.Main - Running application...");
    }

    @Test
    void runtime_only_dependencies_are_not_visible_at_compile_time() {
        build.appBuildFile.appendText("""
            dependencies.constraints {
                javaModuleDependencies {
                    implementation(gav("org.slf4j", "2.0.3"))
                }
            }
        """);
        build.appModuleInfoFile.writeText("""
            module org.gradlex.test.app {
                requires /*runtime*/ org.slf4j;
            
                exports org.gradlex.test.app;
            }""");
        build.file("app/src/main/java/org/gradlex/test/app/Main.java").writeText("""
            package org.gradlex.test.app;
            
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            
            public class Main {
                private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
            
                public static void main(String[] args) {
                    LOGGER.info("Running application...");
                }
            }""");

        var result = build.fail();

        assertThat(result.getOutput()).contains("error: package org.slf4j does not exist");
    }

    @Test
    void generates_javadoc_with_runtime_only_dependencies_in_moduleinfo() {
        build.appBuildFile.appendText("""
            java.withJavadocJar()
            dependencies.constraints {
                javaModuleDependencies {
                    implementation(gav("org.slf4j", "2.0.3"))
                    implementation(gav("org.slf4j.simple", "2.0.3"))
                }
            }""");
        build.appModuleInfoFile.writeText("""
            module org.gradlex.test.app {
                requires org.slf4j;
                requires /*runtime*/ org.slf4j.simple;
            
                exports org.gradlex.test.app;
            }""");
        build.file("app/src/main/java/org/gradlex/test/app/Main.java").writeText("""
            package org.gradlex.test.app;
            
            public class Main {}""");

        var result = build.build().task(":app:javadoc");

        assertThat(result).isNotNull();
        assertThat(result.getOutcome()).isEqualTo(SUCCESS);
    }

    @Test
    void can_configure_additional_compile_tasks_to_work_with_runtime_only_dependencies() {
        // This is typically needed for whitebox testing
        build.appBuildFile.appendText("""
            javaModuleDependencies.addRequiresRuntimeSupport(sourceSets.main.get(), sourceSets.test.get())
            tasks.compileTestJava {
                classpath += sourceSets.main.get().output
            
                val srcDir = sourceSets.test.get().java.sourceDirectories.first()
                options.compilerArgumentProviders.add {
                    listOf(
                        "--module-path",
                        classpath.files.joinToString(":"),
                        "--patch-module",
                        "org.gradlex.test.app=" + srcDir,
                        "--add-modules",
                        "org.junit.jupiter.api",
                        "--add-reads",
                        "org.gradlex.test.app=org.junit.jupiter.api"
                    )
                }
            }
            tasks.test { useJUnitPlatform() }
            
            dependencies.constraints {
                javaModuleDependencies {
                    implementation(gav("org.slf4j", "2.0.3"))
                    implementation(gav("org.slf4j.simple", "2.0.3"))
                }
            }
            dependencies {
                javaModuleDependencies {
                    testImplementation(gav("org.junit.jupiter.api", "5.13.4"))
                    testRuntimeOnly(ga("org.junit.jupiter.engine"))
                    testRuntimeOnly(ga("org.junit.platform.launcher"))
                }
            }
            """);
        build.appModuleInfoFile.writeText("""
            module org.gradlex.test.app {
                requires org.slf4j;
                requires /*runtime*/ org.slf4j.simple;
            }""");
        build.file("app/src/test/java/org/gradlex/test/app/MainTest.java").writeText("""
            package org.gradlex.test.app;
            
            public class MainTest {
                @org.junit.jupiter.api.Test
                void test() {}
            }""");

        var result = build.build().task(":app:compileTestJava");

        assertThat(result).isNotNull();
        assertThat(result.getOutcome()).isEqualTo(SUCCESS);
    }
}
