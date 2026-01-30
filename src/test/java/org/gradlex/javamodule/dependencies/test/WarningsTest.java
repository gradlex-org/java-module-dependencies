// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild;
import org.junit.jupiter.api.Test;

class WarningsTest {

    GradleBuild build = new GradleBuild();

    @Test
    void prints_warning_for_missing_mapping() {
        build.appModuleInfoFile.writeText("""
            module org.my.app {
                requires commons.math3;
            }""");

        var result = build.fail();

        assertThat(result.getOutput())
                .contains("[WARN] [Java Module Dependencies] commons.math3=group:artifact missing in");
    }
}
