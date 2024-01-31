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

package org.gradlex.javamodule.dependencies.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        private final String fullId;

        public DependencyDeclaration(String scope, String moduleName, String fullId) {
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

    public void addDependencies(String name, List<DependencyDeclaration> api, List<DependencyDeclaration> implementation, List<DependencyDeclaration> compileOnlyApi, List<DependencyDeclaration> compileOnly, List<DependencyDeclaration> runtimeOnly) {
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
        Optional<String> dependenciesBlock = fileContentToPreserve.stream().filter(line -> line.contains("dependencies")).findFirst();
        if (dependenciesBlock.isPresent()) {
            fileContentToPreserve = fileContentToPreserve.subList(0, fileContentToPreserve.indexOf(dependenciesBlock.get()));
        }

        List<String> content = new ArrayList<>(fileContentToPreserve);
        if (!content.get(content.size() - 1).isEmpty()) {
            content.add("");
        }

        if (!getDependencies().get().isEmpty()) {
            content.add("dependencies {");
            getDependencies().get().stream().sorted((a, b) -> ("main".equals(a.name)) ? -1 : a.name.compareTo(b.name)).forEach(sourceSetBlock -> {
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
        return dependencies.get().stream().map(d -> {
            String group = d.fullId.split(":")[0];
            String artifact = d.fullId.split(":")[1];
            String feature = null;
            if (artifact.contains("|")) {
                feature = artifact.split("\\|")[1];
                artifact = artifact.split("\\|")[0];
            }

            String identifier;
            if (group.equals(getOwnProjectGroup().get())) {
                if (getWithCatalog().get()) {
                    identifier = "projects." + artifact;
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
                return "    " + d.scope + "(" + identifier + ") { capabilities { requireCapabilities(\"" + group + ":" + artifact + "-" + feature + "\") } }";
            }
        }).collect(Collectors.toList());
    }
}
