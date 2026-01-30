// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.test.provides;

import static org.assertj.core.api.Assertions.assertThat;

import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild;
import org.junit.jupiter.api.Test;

class GenerateMetaInfServicesTest {

    GradleBuild build = new GradleBuild(true);

    @Test
    void generates_meta_inf_services_files() {
        build.libBuildFile.appendText("""
            javaModuleDependencies { generateMetaInfServices() }
            dependencies.constraints {
                implementation("org.junit.platform:junit-platform-engine:6.0.2")
                testFixturesImplementation("org.junit.platform:junit-platform-engine:6.0.2")
            }
        """);
        build.file("lib/src/main/java/abc/lib/NoOpTestEngine.java").writeText("""
                package abc.lib;
                import org.junit.platform.engine.*;
                public class NoOpTestEngine implements TestEngine {
                    public String getId() { return "noop"; }
                    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) { return null; };
                    public void execute(ExecutionRequest request) {};
                }
                """);
        build.file("lib/src/testFixtures/java/abc/lib/fixtures/NoOpTestEngine.java")
                .writeText("""
                package abc.lib.fixtures;
                import org.junit.platform.engine.*;
                public class NoOpTestEngine implements TestEngine {
                    public String getId() { return "noop"; }
                    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) { return null; };
                    public void execute(ExecutionRequest request) {};
                }
                """);

        build.libModuleInfoFile.writeText("""
                module abc.lib {
                    requires org.junit.platform.engine;
                    provides org.junit.platform.engine.TestEngine
                        with abc.lib.NoOpTestEngine;
                }
                """);
        build.file("lib/src/testFixtures/java/module-info.java").writeText("""
                module abc.lib.test.fixtures {
                    requires org.junit.platform.engine;
                    provides org.junit.platform.engine.TestEngine
                        with abc.lib.fixtures.NoOpTestEngine;
                }
                """);

        var result = build.runner("jar", "testFixturesJar").build();
        BuildTask libMain = result.task(":lib:generateMetaInfServices");
        BuildTask libTestFixtures = result.task(":lib:generateTestFixturesMetaInfServices");
        BuildTask appMain = result.task(":app:generateMetaInfServices");
        assertThat(libMain).isNotNull();
        assertThat(libMain.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(libTestFixtures).isNotNull();
        assertThat(libTestFixtures.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(appMain).isNull();

        assertThat(build.file("lib/build/resources/main/META-INF/services/org.junit.platform.engine.TestEngine")
                        .getAsPath())
                .hasContent("abc.lib.NoOpTestEngine");
        assertThat(build.file("lib/build/resources/testFixtures/META-INF/services/org.junit.platform.engine.TestEngine")
                        .getAsPath())
                .hasContent("abc.lib.fixtures.NoOpTestEngine");
    }
}
