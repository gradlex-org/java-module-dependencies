// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.internal.utils;

import static org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE;
import static org.gradle.api.attributes.Category.LIBRARY;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.VersionConstraint;
import org.gradle.api.attributes.Category;
import org.gradle.api.capabilities.Capability;
import org.gradle.api.provider.Provider;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class DependencyDeclarationsUtil {

    public static Provider<List<String>> declaredDependencies(Project project, String configuration) {
        ConfigurationContainer configurations = project.getConfigurations();
        return project.provider(() -> configurations.getNames().contains(configuration)
                ? configurations.getByName(configuration).getDependencies().stream()
                        .filter(DependencyDeclarationsUtil::isLibraryDependency)
                        .map(d -> toIdentifier(project, d))
                        .collect(Collectors.toList())
                : Collections.emptyList());
    }

    private static String toIdentifier(Project project, Dependency dependency) {
        if (dependency instanceof ProjectDependency) {
            // assume Module Name of local Module
            ProjectDependency projectDependency = (ProjectDependency) dependency;
            if (projectDependency.getRequestedCapabilities().isEmpty()) {
                return project.getGroup() + "." + dependency.getName();
            } else {
                Capability capability =
                        projectDependency.getRequestedCapabilities().get(0);
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

    /**
     * Fill a MutableVersionConstraint with the information from another VersionConstraint object retrieved
     * from a version catalog.
     */
    public static void copyVersionConstraint(VersionConstraint version, MutableVersionConstraint copy) {
        String branch = version.getBranch();
        String requiredVersion = version.getRequiredVersion();
        String preferredVersion = version.getPreferredVersion();
        String strictVersion = version.getStrictVersion();

        if (branch != null && !branch.isEmpty()) {
            copy.setBranch(branch);
        }
        if (!requiredVersion.isEmpty()) {
            copy.require(requiredVersion);
        }
        if (!preferredVersion.isEmpty()) {
            copy.prefer(preferredVersion);
        }
        if (!strictVersion.isEmpty()) {
            copy.strictly(strictVersion);
        }
    }
}
