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

package org.gradlex.javamodule.dependencies.internal.bridges;

import com.autonomousapps.DependencyAnalysisSubExtension;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradlex.javamodule.dependencies.JavaModuleDependenciesExtension;
import org.gradlex.javamodule.dependencies.tasks.ModuleDirectivesScopeCheck;

public class DependencyAnalysisBridge {

    public static void registerDependencyAnalysisPostProcessingTask(Project project, TaskProvider<Task> checkAllModuleInfo, JavaModuleDependenciesExtension javaModuleDependencies) {
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);

        TaskProvider<ModuleDirectivesScopeCheck> checkModuleDirectivesScope =
                project.getTasks().register("checkModuleDirectivesScope", ModuleDirectivesScopeCheck.class, javaModuleDependencies);

        sourceSets.all(sourceSet -> checkModuleDirectivesScope.configure(t -> {
            t.getSourceSets().put(
                    sourceSet.getName(),
                    project.getLayout().getProjectDirectory().getAsFile().getParentFile().toPath().relativize(
                            sourceSet.getJava().getSrcDirs().iterator().next().toPath()).resolve("module-info.java").toString());
        }));

        project.getExtensions().getByType(DependencyAnalysisSubExtension.class)
                .registerPostProcessingTask(checkModuleDirectivesScope);

        checkAllModuleInfo.configure(t -> t.dependsOn(checkModuleDirectivesScope));
    }
}
