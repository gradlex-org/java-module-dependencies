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

package org.gradlex.javamodule.dependencies.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.NonNullApi;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradlex.javamodule.dependencies.internal.utils.ModuleInfoClassCreator;

@CacheableTask
public abstract class SyntheticModuleInfoFoldersGenerate extends DefaultTask {

    @Input
    public abstract ListProperty<String> getModuleNames();

    @OutputDirectory
    public abstract DirectoryProperty getSyntheticModuleInfoFolder();

    @TaskAction
    public void generate() {
        for (String moduleName : getModuleNames().get()) {
            ModuleInfoClassCreator.createEmpty(getSyntheticModuleInfoFolder().get().dir(moduleName).getAsFile());
        }
    }
}
