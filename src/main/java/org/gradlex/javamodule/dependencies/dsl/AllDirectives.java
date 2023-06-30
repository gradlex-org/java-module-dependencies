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

import org.gradle.api.tasks.SourceSet;
import org.gradlex.javamodule.dependencies.JavaModuleDependenciesExtension;

abstract public class AllDirectives extends GradleOnlyDirectives {

    public AllDirectives(SourceSet sourceSet, SourceSet mainSourceSet, JavaModuleDependenciesExtension javaModuleDependencies) {
        super(sourceSet, mainSourceSet, javaModuleDependencies);
    }

    public void requires(String moduleName) {
        runtimeClasspathModules.add(moduleName);
        compileClasspathModules.add(moduleName);
        add(sourceSet.getImplementationConfigurationName(), moduleName);
    }

    public void requiresTransitive(String moduleName) {
        runtimeClasspathModules.add(moduleName);
        compileClasspathModules.add(moduleName);
        add(sourceSet.getApiConfigurationName(), moduleName);
    }

    public void requiresStatic(String moduleName) {
        compileClasspathModules.add(moduleName);
        add(sourceSet.getCompileOnlyConfigurationName(), moduleName);
    }

    public void requiresStaticTransitive(String moduleName) {
        compileClasspathModules.add(moduleName);
        add(sourceSet.getCompileOnlyApiConfigurationName(), moduleName);
    }
}
