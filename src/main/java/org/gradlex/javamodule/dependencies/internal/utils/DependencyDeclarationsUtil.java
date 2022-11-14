/*
 * Copyright 2022 the GradleX team.
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

package org.gradlex.javamodule.dependencies.internal.utils;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.attributes.Category;
import org.gradle.api.capabilities.Capability;
import org.gradle.api.provider.Provider;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE;
import static org.gradle.api.attributes.Category.LIBRARY;

public abstract class DependencyDeclarationsUtil {

    public static Provider<List<String>> declaredDependencies(Project project, String configuration) {
        ConfigurationContainer configurations = project.getConfigurations();
        return project.provider(() -> configurations.getNames().contains(configuration)
                ? configurations.getByName(configuration).getDependencies().stream()
                    .filter(DependencyDeclarationsUtil::isLibraryDependency)
                    .map(DependencyDeclarationsUtil::toIdentifier).collect(Collectors.toList())
                : Collections.emptyList());
    }

    private static String toIdentifier(Dependency dependency) {
        if (dependency instanceof ProjectDependency) {
            // assume Module Name of local Module
            ProjectDependency projectDependency = (ProjectDependency) dependency;
            if (projectDependency.getRequestedCapabilities().isEmpty()) {
                return projectDependency.getDependencyProject().getGroup() + "." +  dependency.getName();
            } else {
                Capability capability = projectDependency.getRequestedCapabilities().get(0);
                return capability.getGroup() + "." + capability.getName().replace("-", ".");
            }
        }
        return dependency.getGroup() + ":" + dependency.getName();
    }

    private static boolean isLibraryDependency(Dependency dependency) {
        if (dependency instanceof ModuleDependency) {
            ModuleDependency moduleDependency = (ModuleDependency) dependency;
            Category category = moduleDependency.getAttributes().getAttribute(CATEGORY_ATTRIBUTE);
            return category == null || category.getName().equals(LIBRARY);
        }
        return false;
    }
}
