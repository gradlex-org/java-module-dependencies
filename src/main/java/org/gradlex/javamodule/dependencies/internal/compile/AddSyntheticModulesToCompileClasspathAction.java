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
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradlex.javamodule.dependencies.internal.utils.ModuleInfoClassCreator;

import javax.inject.Inject;
import java.io.File;

@NonNullApi
public abstract class AddSyntheticModulesToCompileClasspathAction implements Action<Task> {

    private final FileCollection syntheticModuleInfoFolders;

    @Inject
    public AddSyntheticModulesToCompileClasspathAction(FileCollection syntheticModuleInfoFolders) {
        this.syntheticModuleInfoFolders = syntheticModuleInfoFolders;
    }

    @Override
    public void execute(Task task) {
        if (syntheticModuleInfoFolders.isEmpty()) {
            return;
        }

        for (File moduleFolder : syntheticModuleInfoFolders) {
            ModuleInfoClassCreator.createEmpty(moduleFolder);
        }

        if (task instanceof JavaCompile) {
            JavaCompile javaCompile = (JavaCompile) task;
            javaCompile.setClasspath(javaCompile.getClasspath().plus(syntheticModuleInfoFolders));
        }
        if (task instanceof Javadoc) {
            Javadoc javadoc = (Javadoc) task;
            javadoc.setClasspath(javadoc.getClasspath().plus(syntheticModuleInfoFolders));
        }
    }
}
