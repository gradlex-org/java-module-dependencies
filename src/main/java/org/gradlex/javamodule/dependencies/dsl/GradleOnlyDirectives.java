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

package org.gradlex.javamodule.dependencies.dsl;

import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.tasks.SourceSet;
import org.gradlex.javamodule.dependencies.JavaModuleDependenciesExtension;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public abstract class GradleOnlyDirectives {

    protected final SourceSet sourceSet;
    protected final SourceSet mainSourceSet;
    protected final JavaModuleDependenciesExtension javaModuleDependencies;

    protected final List<String> compileClasspathModules = new ArrayList<>();
    protected final List<String> runtimeClasspathModules = new ArrayList<>();

    @Inject
    protected abstract DependencyHandler getDependencies();

    public GradleOnlyDirectives(SourceSet sourceSet, SourceSet mainSourceSet, JavaModuleDependenciesExtension javaModuleDependencies) {
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
