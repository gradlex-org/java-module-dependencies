/*
 * Copyright 2022 the GradleX team.
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

package org.gradlex.javamodule.dependencies.internal.compile;

import org.gradle.api.Action;
import org.gradle.api.NonNullApi;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradlex.javamodule.dependencies.internal.utils.ModuleInfoClassCreator;

import javax.inject.Inject;
import java.io.File;
import java.util.List;

@NonNullApi
public abstract class AddSyntheticModulesToCompileClasspathAction implements Action<Task> {

    private final File tmpFolder;
    private final List<String> moduleDependencies;
    private final ObjectFactory objects;

    @Inject
    public AddSyntheticModulesToCompileClasspathAction(File tmpFolder, List<String> moduleDependencies, ObjectFactory objects) {
        this.tmpFolder = tmpFolder;
        this.moduleDependencies = moduleDependencies;
        this.objects = objects;
    }

    @Override
    public void execute(Task task) {
        if (moduleDependencies.isEmpty()) {
            return;
        }

        JavaCompile javaCompile = (JavaCompile) task;

        ConfigurableFileCollection syntheticModuleInfoFolders = objects.fileCollection();
        for (String moduleName : moduleDependencies) {
            File dir = new File(tmpFolder, "java-module-dependencies/" + moduleName + "-synthetic");
            ModuleInfoClassCreator.createEmpty(moduleName, dir);
            syntheticModuleInfoFolders.from(dir);
        }

        javaCompile.setClasspath(javaCompile.getClasspath().plus(syntheticModuleInfoFolders));
    }
}
