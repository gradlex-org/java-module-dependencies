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

package org.gradlex.javamodule.dependencies.internal.bridges;

import com.autonomousapps.AbstractExtension;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradlex.javamodule.dependencies.tasks.ModuleDirectivesScopeCheck;

import java.io.File;

public class DependencyAnalysisBridge {

    public static void registerDependencyAnalysisPostProcessingTask(Project project, TaskProvider<Task> checkAllModuleInfo) {
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);

        TaskProvider<ModuleDirectivesScopeCheck> checkModuleDirectivesScope =
                project.getTasks().register("checkModuleDirectivesScope", ModuleDirectivesScopeCheck.class);

        sourceSets.all(sourceSet -> checkModuleDirectivesScope.configure(t -> {
            File moduleInfo = new File(sourceSet.getJava().getSrcDirs().iterator().next(), "module-info.java");
            if (!moduleInfo.exists()) {
                moduleInfo = project.getBuildFile(); // no module-info: dependencies are declared in build file
            }
            t.getSourceSets().put(sourceSet.getName(), moduleInfo.getAbsolutePath());

            Configuration cpClasspath = project.getConfigurations().getByName(sourceSet.getCompileClasspathConfigurationName());
            Configuration rtClasspath = project.getConfigurations().getByName(sourceSet.getRuntimeClasspathConfigurationName());
            t.getModuleArtifacts().add(project.provider(() -> cpClasspath.getIncoming().getArtifacts()));
            t.getModuleArtifacts().add(project.provider(() -> rtClasspath.getIncoming().getArtifacts()));
        }));

        project.getExtensions().getByType(AbstractExtension.class)
                .registerPostProcessingTask(checkModuleDirectivesScope);

        checkAllModuleInfo.configure(t -> t.dependsOn(checkModuleDirectivesScope));
    }
}
