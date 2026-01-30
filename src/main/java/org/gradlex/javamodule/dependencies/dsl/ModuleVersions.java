// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.dsl;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencyConstraint;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradlex.javamodule.dependencies.JavaModuleDependenciesExtension;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class ModuleVersions {

    private final Map<String, String> declaredVersions = new LinkedHashMap<>();
    private final Configuration configuration;
    protected final JavaModuleDependenciesExtension javaModuleDependencies;

    public Map<String, String> getDeclaredVersions() {
        return declaredVersions;
    }

    @Inject
    protected abstract DependencyHandler getDependencies();

    public ModuleVersions(Configuration configuration, JavaModuleDependenciesExtension javaModuleDependencies) {
        this.configuration = configuration;
        this.javaModuleDependencies = javaModuleDependencies;
    }

    public void version(String moduleName, String version) {
        version(moduleName, version, a -> {});
    }

    public void version(String moduleName, Action<? super MutableVersionConstraint> version) {
        version(moduleName, "", version);
    }

    public void version(String moduleName, String requiredVersion, Action<? super MutableVersionConstraint> version) {
        getDependencies()
                .getConstraints()
                .add(
                        configuration.getName(),
                        javaModuleDependencies.ga(moduleName).map(ga -> {
                            String mainComponentCoordinates;
                            if (ga.contains("|")) {
                                mainComponentCoordinates = ga.substring(0, ga.indexOf("|")) + ":" + requiredVersion;
                            } else {
                                mainComponentCoordinates = ga + ":" + requiredVersion;
                            }
                            DependencyConstraint dependencyConstraint =
                                    getDependencies().getConstraints().create(mainComponentCoordinates);
                            dependencyConstraint.version(version);
                            return dependencyConstraint;
                        }));
        declaredVersions.put(moduleName, requiredVersion);
    }
}
