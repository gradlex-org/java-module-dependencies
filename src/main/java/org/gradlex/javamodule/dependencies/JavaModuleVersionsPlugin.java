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
import org.gradle.api.attributes.Usage;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradlex.javamodule.dependencies.dsl.ModuleVersions;

import static org.gradle.api.attributes.Usage.JAVA_RUNTIME;
import static org.gradle.api.plugins.JavaPlatformPlugin.API_CONFIGURATION_NAME;

@SuppressWarnings("unused")
@NonNullApi
public abstract class JavaModuleVersionsPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPlugins().withType(JavaPlatformPlugin.class, plugin -> setupForJavaPlatformProject(project));
        project.getPlugins().withType(JavaPlugin.class, plugin -> setupForJavaProject(project));
    }

    private void setupForJavaPlatformProject(Project project) {
        setupVersionsDSL(project, project.getConfigurations().getByName(API_CONFIGURATION_NAME));
    }

    private void setupForJavaProject(Project project) {
        ObjectFactory objects = project.getObjects();
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);

        Configuration versions = project.getConfigurations().create("versions", c -> {
            c.setCanBeResolved(false);
            c.setCanBeConsumed(false);
        });

        Configuration platformElements = project.getConfigurations().create("platformElements", c -> {
            c.setCanBeResolved(false);
            c.setCanBeConsumed(true);
            c.setVisible(false);
            c.extendsFrom(versions);
            c.getAttributes().attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, JAVA_RUNTIME));
        });

        // https://github.com/gradle/gradle/issues/26163
        project.afterEvaluate(p -> platformElements.getOutgoing().capability(project.getGroup() + ":" + project.getName() + "-platform:" + project.getVersion()));

        setupVersionsDSL(project, versions);
    }

    private void setupVersionsDSL(Project project, Configuration configuration) {
        project.getPlugins().apply(JavaModuleDependenciesPlugin.class);
        JavaModuleDependenciesExtension javaModuleDependencies = project.getExtensions().getByType(JavaModuleDependenciesExtension.class);
        project.getExtensions().create("moduleInfo", ModuleVersions.class, configuration, javaModuleDependencies);
    }

}
