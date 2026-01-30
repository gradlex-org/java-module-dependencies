// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class BuildFileDependenciesGenerate extends DefaultTask {
    public abstract static class SourceSetDependencies {

        private final String name;

        @Inject
        public SourceSetDependencies(String name) {
            this.name = name;
        }

        public abstract ListProperty<DependencyDeclaration> getApiDependencies();

        public abstract ListProperty<DependencyDeclaration> getImplementationDependencies();

        public abstract ListProperty<DependencyDeclaration> getCompileOnlyApiDependencies();

        public abstract ListProperty<DependencyDeclaration> getCompileOnlyDependencies();

        public abstract ListProperty<DependencyDeclaration> getRuntimeOnlyDependencies();

        private boolean isEmpty() {
            return getApiDependencies().get().isEmpty()
                    && getImplementationDependencies().get().isEmpty()
                    && getCompileOnlyApiDependencies().get().isEmpty()
                    && getCompileOnlyDependencies().get().isEmpty()
                    && getRuntimeOnlyDependencies().get().isEmpty();
        }
    }

    public static class DependencyDeclaration {
        private final String scope;
        private final String moduleName;
        private final Provider<String> fullId;

        public DependencyDeclaration(String scope, String moduleName, Provider<String> fullId) {
            this.scope = scope;
            this.moduleName = moduleName;
            this.fullId = fullId;
        }
    }

    @Internal
    public abstract ListProperty<SourceSetDependencies> getDependencies();

    @Internal
    public abstract Property<Boolean> getWithCatalog();

    @Internal
    public abstract Property<String> getOwnProjectGroup();

    @Internal
    public abstract RegularFileProperty getBuildFile();

    @Inject
    public abstract ObjectFactory getObjects();

    public void addDependencies(
            String name,
            List<DependencyDeclaration> api,
            List<DependencyDeclaration> implementation,
            List<DependencyDeclaration> compileOnlyApi,
            List<DependencyDeclaration> compileOnly,
            List<DependencyDeclaration> runtimeOnly) {
        SourceSetDependencies dependencies = getObjects().newInstance(SourceSetDependencies.class, name);
        dependencies.getApiDependencies().convention(api);
        dependencies.getImplementationDependencies().convention(implementation);
        dependencies.getCompileOnlyApiDependencies().convention(compileOnlyApi);
        dependencies.getCompileOnlyDependencies().convention(compileOnly);
        dependencies.getRuntimeOnlyDependencies().convention(runtimeOnly);
        getDependencies().add(dependencies);
    }

    @TaskAction
    public void generate() throws IOException {
        File buildGradle = getBuildFile().get().getAsFile();
        List<String> fileContentToPreserve = Files.readAllLines(buildGradle.toPath());
        Optional<String> dependenciesBlock = fileContentToPreserve.stream()
                .filter(line -> line.contains("dependencies"))
                .findFirst();
        if (dependenciesBlock.isPresent()) {
            fileContentToPreserve =
                    fileContentToPreserve.subList(0, fileContentToPreserve.indexOf(dependenciesBlock.get()));
        }

        List<String> content = new ArrayList<>(fileContentToPreserve);
        if (!content.get(content.size() - 1).isEmpty()) {
            content.add("");
        }

        if (!getDependencies().get().stream().allMatch(SourceSetDependencies::isEmpty)) {
            content.add("dependencies {");
            getDependencies().get().stream()
                    .sorted((a, b) -> ("main".equals(a.name)) ? -1 : a.name.compareTo(b.name))
                    .forEach(sourceSetBlock -> {
                        content.addAll(toDeclarationString(sourceSetBlock.getApiDependencies()));
                        content.addAll(toDeclarationString(sourceSetBlock.getImplementationDependencies()));
                        content.addAll(toDeclarationString(sourceSetBlock.getCompileOnlyApiDependencies()));
                        content.addAll(toDeclarationString(sourceSetBlock.getCompileOnlyDependencies()));
                        content.addAll(toDeclarationString(sourceSetBlock.getRuntimeOnlyDependencies()));
                        if (!sourceSetBlock.isEmpty()) {
                            content.add("");
                        }
                    });
            content.remove(content.size() - 1);
            content.add("}");
        }

        Files.write(buildGradle.toPath(), content);
    }

    private List<String> toDeclarationString(ListProperty<DependencyDeclaration> dependencies) {
        return dependencies.get().stream()
                .map(d -> {
                    String group = d.fullId.get().split(":")[0];
                    String artifact = d.fullId.get().split(":")[1];
                    String feature = null;
                    if (artifact.contains("|")) {
                        feature = artifact.split("\\|")[1];
                        artifact = artifact.split("\\|")[0];
                    }

                    String identifier;
                    if (group.equals(getOwnProjectGroup().get())) {
                        if (getWithCatalog().get()) {
                            identifier = "projects." + toCamelCase(artifact);
                        } else {
                            identifier = "project(\":" + artifact + "\")";
                        }
                    } else {
                        if (getWithCatalog().get()) {
                            identifier = "libs." + d.moduleName;
                        } else {
                            identifier = "\"" + group + ":" + artifact + "\"";
                        }
                    }

                    if (feature == null) {
                        return "    " + d.scope + "(" + identifier + ")";
                    } else {
                        return "    " + d.scope + "(" + identifier + ") { capabilities { requireCapabilities(\"" + group
                                + ":" + artifact + "-" + feature + "\") } }";
                    }
                })
                .collect(Collectors.toList());
    }

    private String toCamelCase(String s) {
        String[] segments = s.split("[\\W_]+");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            String word = segments[i];
            if (i == 0) {
                word = word.isEmpty() ? word : word.toLowerCase();
            } else {
                word = word.isEmpty()
                        ? word
                        : Character.toUpperCase(word.charAt(0))
                                + word.substring(1).toLowerCase();
            }
            builder.append(word);
        }
        return builder.toString();
    }
}
