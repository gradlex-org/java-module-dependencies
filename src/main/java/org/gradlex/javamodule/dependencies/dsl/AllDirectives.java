// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.dsl;

import org.gradle.api.tasks.SourceSet;
import org.gradlex.javamodule.dependencies.JavaModuleDependenciesExtension;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class AllDirectives extends GradleOnlyDirectives {

    public AllDirectives(
            SourceSet sourceSet, SourceSet mainSourceSet, JavaModuleDependenciesExtension javaModuleDependencies) {
        super(sourceSet, mainSourceSet, javaModuleDependencies);
    }

    public void requires(String moduleName) {
        runtimeClasspathModules.add(moduleName);
        compileClasspathModules.add(moduleName);
        add(sourceSet.getImplementationConfigurationName(), moduleName);
    }

    public void requiresStatic(String moduleName) {
        compileClasspathModules.add(moduleName);
        add(sourceSet.getCompileOnlyConfigurationName(), moduleName);
    }

    public void exportsTo(String moduleName) {
        exportsToModules.add(moduleName);
    }

    public void opensTo(String moduleName) {
        opensToModules.add(moduleName);
    }
}
