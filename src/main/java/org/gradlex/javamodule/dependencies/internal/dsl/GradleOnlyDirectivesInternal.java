// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.internal.dsl;

import java.util.List;
import org.gradle.api.tasks.SourceSet;
import org.gradlex.javamodule.dependencies.JavaModuleDependenciesExtension;
import org.gradlex.javamodule.dependencies.dsl.GradleOnlyDirectives;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class GradleOnlyDirectivesInternal extends GradleOnlyDirectives {

    public GradleOnlyDirectivesInternal(
            SourceSet sourceSet, SourceSet mainSourceSet, JavaModuleDependenciesExtension javaModuleDependencies) {
        super(sourceSet, mainSourceSet, javaModuleDependencies);
    }

    public List<String> getCompileClasspathModules() {
        return compileClasspathModules;
    }

    public List<String> getRuntimeClasspathModules() {
        return runtimeClasspathModules;
    }
}
