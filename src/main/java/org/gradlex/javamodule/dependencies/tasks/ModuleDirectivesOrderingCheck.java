// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo;

@CacheableTask
public abstract class ModuleDirectivesOrderingCheck extends DefaultTask {

    @Input
    @Optional
    public abstract Property<String> getModuleInfoPath();

    @Input
    @Optional
    public abstract Property<String> getModuleNamePrefix();

    @Input
    public abstract Property<ModuleInfo> getModuleInfo();

    @OutputFile
    public abstract RegularFileProperty getReport();

    @TaskAction
    public void checkOrder() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (ModuleInfo.Directive directive : ModuleInfo.Directive.values()) {
            List<String> originalOrder = getModuleInfo().get().get(directive).stream()
                    .map(name -> name + ";")
                    .collect(Collectors.toList());

            List<String> sorted = new ArrayList<>(originalOrder);
            sorted.sort((m1, m2) -> {
                // own modules go first
                if (getModuleNamePrefix().isPresent()) {
                    if (m1.startsWith(getModuleNamePrefix().get())
                            && !m2.startsWith(getModuleNamePrefix().get())) {
                        return -1;
                    }
                    if (!m1.startsWith(getModuleNamePrefix().get())
                            && m2.startsWith(getModuleNamePrefix().get())) {
                        return 1;
                    }
                }
                return m1.compareTo(m2);
            });

            if (!originalOrder.equals(sorted)) {
                p(sb, "'" + directive.literal() + "' are not declared in alphabetical order. Please use this order:");
                for (String entry : sorted) {
                    p(sb, "    " + directive.literal() + " " + entry);
                }
                p(sb, "");
            }
        }

        Files.write(getReport().get().getAsFile().toPath(), sb.toString().getBytes());

        if (sb.length() > 0) {
            throw new RuntimeException(getModuleInfoPath().get() + "\n\n" + sb);
        }
    }

    private void p(StringBuilder sb, String toPrint) {
        sb.append(toPrint);
        sb.append("\n");
    }
}
