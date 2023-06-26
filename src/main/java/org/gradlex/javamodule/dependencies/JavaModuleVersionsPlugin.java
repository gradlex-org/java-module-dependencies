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

package org.gradlex.javamodule.dependencies;

import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradlex.javamodule.dependencies.dsl.ModuleVersions;

@SuppressWarnings("unused")
@NonNullApi
public abstract class JavaModuleVersionsPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPlugins().withType(JavaPlugin.class, javaPlugin -> setupForProject(project));
        project.getPlugins().withType(JavaPlatformPlugin.class, javaPlugin -> setupForProject(project));
    }

    private void setupForProject(Project project) {
        project.getPlugins().apply(JavaModuleDependenciesPlugin.class);
        JavaModuleDependenciesExtension javaModuleDependencies = project.getExtensions().getByType(JavaModuleDependenciesExtension.class);
        Configuration configuration = project.getConfigurations().findByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME);
        if (configuration == null) {
            configuration = project.getConfigurations().getByName(JavaPlatformPlugin.API_CONFIGURATION_NAME);
        }
        project.getExtensions().create("moduleInfo", ModuleVersions.class, configuration, javaModuleDependencies);
    }

}
