// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradlex.javamodule.dependencies.internal.utils.ModuleInfoClassCreator;
import org.jspecify.annotations.NullMarked;

@NullMarked
@CacheableTask
public abstract class SyntheticModuleInfoFoldersGenerate extends DefaultTask {

    @Input
    public abstract ListProperty<String> getModuleNames();

    @OutputDirectory
    public abstract DirectoryProperty getSyntheticModuleInfoFolder();

    @TaskAction
    public void generate() {
        for (String moduleName : getModuleNames().get()) {
            ModuleInfoClassCreator.createEmpty(
                    getSyntheticModuleInfoFolder().get().dir(moduleName).getAsFile());
        }
    }
}
