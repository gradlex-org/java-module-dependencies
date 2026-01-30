// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.internal.dsl;

import java.util.List;
import org.gradle.api.tasks.SourceSet;
import org.gradlex.javamodule.dependencies.JavaModuleDependenciesExtension;
import org.gradlex.javamodule.dependencies.dsl.AllDirectives;
import org.jspecify.annotations.NullMarked;

/**
 * Note: These methods are used by the 'java-module-testing' plugin to access information
 * defined in the Module Info DSL.
 */
@NullMarked
public abstract class AllDirectivesInternal extends AllDirectives {

    public AllDirectivesInternal(
            SourceSet sourceSet, SourceSet mainSourceSet, JavaModuleDependenciesExtension javaModuleDependencies) {
        super(sourceSet, mainSourceSet, javaModuleDependencies);
    }

    public List<String> getCompileClasspathModules() {
        return compileClasspathModules;
    }

    public List<String> getRuntimeClasspathModules() {
        return runtimeClasspathModules;
    }

    public List<String> getExportsToModules() {
        return exportsToModules;
    }

    public List<String> getOpensToModules() {
        return opensToModules;
    }
}
