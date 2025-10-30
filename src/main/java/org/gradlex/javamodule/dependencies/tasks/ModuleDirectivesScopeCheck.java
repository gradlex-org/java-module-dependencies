// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.tasks;

import static org.gradle.api.plugins.JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME;
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleJar.readModuleNameFromJarFile;

import com.autonomousapps.AbstractPostProcessingTask;
import com.autonomousapps.model.Advice;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.artifacts.ArtifactCollection;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.capabilities.Capability;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.jspecify.annotations.Nullable;

@CacheableTask
public abstract class ModuleDirectivesScopeCheck extends AbstractPostProcessingTask {

    private static final Map<String, String> SCOPES_TO_DIRECTIVES = new HashMap<>();
    private static final Map<String, String> SCOPES_TO_DIRECTIVES_BUILD_FILE_DSL = new HashMap<>();

    static {
        SCOPES_TO_DIRECTIVES.put("compileOnlyApi", "requires static transitive");
        SCOPES_TO_DIRECTIVES.put("compileOnly", "requires static");
        SCOPES_TO_DIRECTIVES.put("api", "requires transitive");
        SCOPES_TO_DIRECTIVES.put("implementation", "requires");
        SCOPES_TO_DIRECTIVES_BUILD_FILE_DSL.put("compileOnlyApi", "requiresStaticTransitive");
        SCOPES_TO_DIRECTIVES_BUILD_FILE_DSL.put("compileOnly", "requiresStatic");
        SCOPES_TO_DIRECTIVES_BUILD_FILE_DSL.put("api", "requiresTransitive");
        SCOPES_TO_DIRECTIVES_BUILD_FILE_DSL.put("implementation", "requires");
    }

    @Input
    public abstract MapProperty<String, String> getSourceSets();

    @Internal
    public abstract ListProperty<ArtifactCollection> getModuleArtifacts();

    @OutputFile
    public abstract RegularFileProperty getReport();

    @TaskAction
    public void analyze() throws IOException {
        Set<Advice> projectAdvice = projectAdvice().getDependencyAdvice();

        StringBuilder message = new StringBuilder();
        for (Map.Entry<String, String> sourceSet : getSourceSets().get().entrySet()) {
            boolean inBuildFile = !sourceSet.getValue().endsWith("module-info.java");
            List<String> toAdd = projectAdvice.stream()
                    .filter(a -> a.getToConfiguration() != null
                            && !RUNTIME_ONLY_CONFIGURATION_NAME.equals(
                                    getScope(a.getToConfiguration()).orElse(null)))
                    .filter(a -> sourceSet.getKey().equals(sourceSetName(a.getToConfiguration())))
                    .map(a -> declaration(
                            a.getToConfiguration(),
                            a.getCoordinates().getIdentifier(),
                            a.getCoordinates().getGradleVariantIdentification().getCapabilities(),
                            inBuildFile))
                    .sorted()
                    .collect(Collectors.toList());

            List<String> toRemove = projectAdvice.stream()
                    .filter(a -> a.getFromConfiguration() != null)
                    .filter(a -> sourceSet.getKey().equals(sourceSetName(a.getFromConfiguration())))
                    .map(a -> declaration(
                            a.getFromConfiguration(),
                            a.getCoordinates().getIdentifier(),
                            a.getCoordinates().getGradleVariantIdentification().getCapabilities(),
                            inBuildFile))
                    .sorted()
                    .collect(Collectors.toList());

            if (!toAdd.isEmpty() || !toRemove.isEmpty()) {
                if (message.length() > 0) {
                    message.append("\n\n\n");
                }
                message.append(sourceSet.getValue());
            }
            if (!toAdd.isEmpty()) {
                message.append("\n\nPlease add the following requires directives:");
                if (inBuildFile) {
                    message.append("\n  ").append(sourceSet.getKey()).append("ModuleInfo {");
                }
                message.append("\n    ").append(String.join("\n    ", toAdd));
                if (inBuildFile) {
                    message.append("\n  }");
                }
            }
            if (!toRemove.isEmpty()) {
                message.append("\n\nPlease remove the following requires directives (or change to runtimeOnly):");
                if (inBuildFile) {
                    message.append("\n  ").append(sourceSet.getKey()).append("ModuleInfo {");
                }
                message.append("\n    ").append(String.join("\n    ", toRemove));
                if (inBuildFile) {
                    message.append("\n  }");
                }
            }
        }

        Files.write(getReport().get().getAsFile().toPath(), message.toString().getBytes());

        if (message.length() > 0) {
            throw new RuntimeException(message.toString());
        }
    }

    private String declaration(String conf, String coordinates, Set<String> capabilities, boolean inBuildFile) {
        String capability =
                capabilities.isEmpty() ? coordinates : capabilities.iterator().next();
        ResolvedArtifactResult moduleJar = getModuleArtifacts().get().stream()
                .flatMap(c -> c.getArtifacts().stream())
                .filter(a -> coordinatesEquals(coordinates, capability, a))
                .findFirst()
                .orElse(null);
        try {
            String moduleName = null;
            if (moduleJar != null) {
                moduleName = readModuleNameFromJarFile(moduleJar.getFile());
            }
            if (moduleName == null) {
                moduleName = coordinates;
            }
            if (inBuildFile) {
                return directive(conf, SCOPES_TO_DIRECTIVES_BUILD_FILE_DSL) + "(\"" + moduleName + "\")";
            } else {
                return directive(conf, SCOPES_TO_DIRECTIVES) + " " + moduleName + ";";
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean coordinatesEquals(String coordinates, String capability, ResolvedArtifactResult selected) {
        ComponentIdentifier id = selected.getId().getComponentIdentifier();
        List<Capability> capabilities = selected.getVariant().getCapabilities();
        if (capabilities.stream().noneMatch(c -> capability.endsWith(":" + c.getName()))) {
            return false;
        }
        if (id instanceof ModuleComponentIdentifier) {
            return coordinates.equals(
                    ((ModuleComponentIdentifier) id).getModuleIdentifier().toString());
        }
        if (id instanceof ProjectComponentIdentifier) {
            return coordinates.equals(((ProjectComponentIdentifier) id).getProjectPath());
        }
        return false;
    }

    @Nullable
    private String sourceSetName(String configurationName) {
        Optional<String> scope = getScope(configurationName);
        if (!scope.isPresent()) {
            return null;
        }
        String sourceSet = configurationName.substring(
                0, configurationName.length() - scope.get().length());
        return sourceSet.isEmpty() ? "main" : sourceSet;
    }

    @Nullable
    private String directive(String configurationName, Map<String, String> scopesToDirectives) {
        return getScope(configurationName).map(scopesToDirectives::get).orElse(null);
    }

    private Optional<String> getScope(String configurationName) {
        return SCOPES_TO_DIRECTIVES.keySet().stream()
                .filter(k -> configurationName.toLowerCase(Locale.ROOT).endsWith(k.toLowerCase(Locale.ROOT)))
                .findFirst();
    }
}
