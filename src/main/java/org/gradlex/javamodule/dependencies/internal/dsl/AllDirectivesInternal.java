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

package org.gradlex.javamodule.dependencies.internal.dsl;

import org.gradle.api.tasks.SourceSet;
import org.gradlex.javamodule.dependencies.JavaModuleDependenciesExtension;
import org.gradlex.javamodule.dependencies.dsl.AllDirectives;

import java.util.List;

/**
 * Note: These methods are used by the 'java-module-testing' plugin to access information
 * defined in the Module Info DSL.
 */
abstract public class AllDirectivesInternal extends AllDirectives {

    public AllDirectivesInternal(SourceSet sourceSet, SourceSet mainSourceSet, JavaModuleDependenciesExtension javaModuleDependencies) {
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
