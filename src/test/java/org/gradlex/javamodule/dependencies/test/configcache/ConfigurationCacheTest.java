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

package org.gradlex.javamodule.dependencies.test.configcache;

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.util.GradleVersion.version;
import static org.gradlex.javamodule.dependencies.test.fixture.GradleBuild.GRADLE_VERSION_UNDER_TEST;

class ConfigurationCacheTest {

    GradleBuild build = new GradleBuild();

    final String noCacheMessage = GRADLE_VERSION_UNDER_TEST == null || version(GRADLE_VERSION_UNDER_TEST).compareTo(version("8.8")) >= 0
            ? "Calculating task graph as no cached configuration is available for tasks: :app:compileJava"
            : "Calculating task graph as no configuration cache is available for tasks: :app:compileJava";

    @Test
    void configurationCacheHit() {
        build.libModuleInfoFile.writeText("module abc.lib { }");

        build.appModuleInfoFile.writeText("""
            module abc.app {
                requires abc.lib;
            }""");

        var runner = build.runner("--configuration-cache",":app:compileJava");
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

        var runner = build.runner("--configuration-cache",":app:compileJava");
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

        var runner = build.runner("--configuration-cache",":app:compileJava");
        var result = runner.build();

        assertThat(result.getOutput()).contains(noCacheMessage);

        build.appModuleInfoFile.writeText("""
            module abc.app {
               //dependency removed; so thats indeed a configuration change
            }""");
        result = runner.build();

        assertThat(result.getOutput()).contains(
                "Calculating task graph as configuration cache cannot be reused because a build logic input of type 'ValueSourceModuleInfo' has changed.\n");
    }

}
