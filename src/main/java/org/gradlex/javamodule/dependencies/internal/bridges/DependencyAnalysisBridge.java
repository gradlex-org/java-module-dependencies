// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.internal.bridges;

import com.autonomousapps.AbstractExtension;
import java.io.File;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradlex.javamodule.dependencies.tasks.ModuleDirectivesOrderingCheck;
import org.gradlex.javamodule.dependencies.tasks.ModuleDirectivesScopeCheck;
import org.jspecify.annotations.Nullable;

public class DependencyAnalysisBridge {

    public static void registerDependencyAnalysisPostProcessingTask(
            Project project, @Nullable TaskProvider<Task> checkAllModuleInfo) {
        TaskContainer tasks = project.getTasks();
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);

        TaskProvider<ModuleDirectivesScopeCheck> checkModuleDirectivesScope =
                tasks.register("checkModuleDirectivesScope", ModuleDirectivesScopeCheck.class, t -> t.getReport()
                        .convention(project.getLayout()
                                .getBuildDirectory()
                                .file("reports/module-info-analysis/scopes.txt")));

        sourceSets.all(sourceSet -> checkModuleDirectivesScope.configure(t -> {
            File moduleInfo =
                    new File(sourceSet.getJava().getSrcDirs().iterator().next(), "module-info.java");
            if (!moduleInfo.exists()) {
                moduleInfo = project.getBuildFile(); // no module-info: dependencies are declared in build file
            }
            t.getSourceSets().put(sourceSet.getName(), moduleInfo.getAbsolutePath());

            Configuration cpClasspath =
                    project.getConfigurations().getByName(sourceSet.getCompileClasspathConfigurationName());
            Configuration rtClasspath =
                    project.getConfigurations().getByName(sourceSet.getRuntimeClasspathConfigurationName());
            t.getModuleArtifacts()
                    .add(project.provider(() -> cpClasspath.getIncoming().getArtifacts()));
            t.getModuleArtifacts()
                    .add(project.provider(() -> rtClasspath.getIncoming().getArtifacts()));
        }));

        project.getExtensions()
                .getByType(AbstractExtension.class)
                .registerPostProcessingTask(checkModuleDirectivesScope);

        if (checkAllModuleInfo != null) {
            checkAllModuleInfo.configure(t -> t.dependsOn(checkModuleDirectivesScope));
        }
        tasks.withType(ModuleDirectivesOrderingCheck.class)
                .configureEach(t -> t.mustRunAfter(checkModuleDirectivesScope));
    }
}
