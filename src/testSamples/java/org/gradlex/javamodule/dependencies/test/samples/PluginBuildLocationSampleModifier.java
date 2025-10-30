// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.test.samples;

import java.io.File;
import java.util.Arrays;
import org.gradle.exemplar.model.Command;
import org.gradle.exemplar.model.Sample;
import org.gradle.exemplar.test.runner.SampleModifier;

public class PluginBuildLocationSampleModifier implements SampleModifier {
    @Override
    public Sample modify(Sample sampleIn) {
        Command cmd = sampleIn.getCommands().remove(0);
        File pluginProjectDir = new File(".");
        sampleIn.getCommands()
                .add(new Command(
                        new File(pluginProjectDir, "gradlew").getAbsolutePath(),
                        cmd.getExecutionSubdirectory(),
                        Arrays.asList("build", "run", "-PpluginLocation=" + pluginProjectDir.getAbsolutePath()),
                        cmd.getFlags(),
                        cmd.getExpectedOutput(),
                        cmd.isExpectFailure(),
                        true,
                        cmd.isAllowDisorderedOutput(),
                        cmd.getUserInputs()));
        return sampleIn;
    }
}
