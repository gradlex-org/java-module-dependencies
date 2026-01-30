// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.test.configcache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.util.GradleVersion.version;
import static org.gradlex.javamodule.dependencies.test.fixture.GradleBuild.GRADLE_VERSION_UNDER_TEST;

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("no-cross-version") // can be removed once the min version is increased to 7.6.5
class ConfigurationCacheTest {

    GradleBuild build = new GradleBuild();

    final String noCacheMessage = GRADLE_VERSION_UNDER_TEST == null
                    || version(GRADLE_VERSION_UNDER_TEST).compareTo(version("8.8")) >= 0
            ? "Calculating task graph as no cached configuration is available for tasks: :app:compileJava"
            : "Calculating task graph as no configuration cache is available for tasks: :app:compileJava";

    @Test
    void configurationCacheHit() {
        build.libModuleInfoFile.writeText("module abc.lib { }");

        build.appModuleInfoFile.writeText("""
            module abc.app {
                requires abc.lib;
            }""");

        var runner = build.runner("--configuration-cache", ":app:compileJava");
        var result = runner.build();

        assertThat(result.getOutput()).contains(noCacheMessage);

        result = runner.build();

        assertThat(result.getOutput()).contains("Reusing configuration cache.");
    }

    @Test
    void configurationCacheHitIrrelevantChange() {
        build.libModuleInfoFile.writeText("module abc.lib { }");
        build.appModuleInfoFile.writeText("""
            module abc.app {
                requires abc.lib;
            }""");

        var runner = build.runner("--configuration-cache", ":app:compileJava");
        var result = runner.build();

        assertThat(result.getOutput()).contains(noCacheMessage);

        build.appModuleInfoFile.writeText("""
            module abc.app {
                requires abc.lib; //This is a comment and should not break the configurationCache
            }
        """);
        result = runner.build();

        assertThat(result.getOutput()).contains("Reusing configuration cache.");
    }

    @Test
    void configurationCacheMissRelevantChange() {
        build.libModuleInfoFile.writeText("module abc.lib { }");
        build.appModuleInfoFile.writeText("""
            module abc.app {
                requires abc.lib;
            }""");

        var runner = build.runner("--configuration-cache", ":app:compileJava");
        var result = runner.build();

        assertThat(result.getOutput()).contains(noCacheMessage);

        build.appModuleInfoFile.writeText("""
            module abc.app {
               //dependency removed; so thats indeed a configuration change
            }""");
        result = runner.build();

        assertThat(result.getOutput())
                .contains(
                        "Calculating task graph as configuration cache cannot be reused because a build logic input of type 'ValueSourceModuleInfo' has changed.\n");
    }
}
