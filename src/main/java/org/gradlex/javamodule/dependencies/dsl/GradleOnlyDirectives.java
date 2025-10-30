// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.dsl;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.tasks.SourceSet;
import org.gradlex.javamodule.dependencies.JavaModuleDependenciesExtension;

public abstract class GradleOnlyDirectives {

    protected final SourceSet sourceSet;
    protected final SourceSet mainSourceSet;
    protected final JavaModuleDependenciesExtension javaModuleDependencies;

    protected final List<String> compileClasspathModules = new ArrayList<>();
    protected final List<String> runtimeClasspathModules = new ArrayList<>();
    protected final List<String> exportsToModules = new ArrayList<>();
    protected final List<String> opensToModules = new ArrayList<>();

    @Inject
    protected abstract DependencyHandler getDependencies();

    public GradleOnlyDirectives(
            SourceSet sourceSet, SourceSet mainSourceSet, JavaModuleDependenciesExtension javaModuleDependencies) {
        this.sourceSet = sourceSet;
        this.mainSourceSet = mainSourceSet;
        this.javaModuleDependencies = javaModuleDependencies;
    }

    protected void add(String scope, String moduleName) {
        getDependencies().addProvider(scope, javaModuleDependencies.create(moduleName, mainSourceSet));
    }

    public void runtimeOnly(String moduleName) {
        runtimeClasspathModules.add(moduleName);
        add(sourceSet.getRuntimeOnlyConfigurationName(), moduleName);
    }

    public void annotationProcessor(String moduleName) {
        add(sourceSet.getAnnotationProcessorConfigurationName(), moduleName);
    }
}
