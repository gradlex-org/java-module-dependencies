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

package org.gradlex.javamodule.dependencies.tasks;

import com.autonomousapps.AbstractPostProcessingTask;
import com.autonomousapps.model.Advice;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradlex.javamodule.dependencies.JavaModuleDependenciesExtension;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.gradle.api.plugins.JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME;

public abstract class ModuleDirectivesScopeCheck extends AbstractPostProcessingTask {

    private static final Map<String, String> SCOPES_TO_DIRECTIVES = new HashMap<>();
    static {
        SCOPES_TO_DIRECTIVES.put("compileOnlyApi", "requires static transitive");
        SCOPES_TO_DIRECTIVES.put("compileOnly", "requires static");
        SCOPES_TO_DIRECTIVES.put("api", "requires transitive");
        SCOPES_TO_DIRECTIVES.put("implementation", "requires");
        SCOPES_TO_DIRECTIVES.put("runtimeOnly", "requires /*runtime*/");
    }

    private final JavaModuleDependenciesExtension javaModuleDependencies;

    @Input
    public abstract MapProperty<String, String> getSourceSets();

    @Inject
    public ModuleDirectivesScopeCheck(JavaModuleDependenciesExtension javaModuleDependencies) {
        this.javaModuleDependencies = javaModuleDependencies;
    }

    @TaskAction
    public void analyze() {
        Set<Advice> projectAdvice = projectAdvice().getDependencyAdvice();

        StringBuilder message = new StringBuilder();
        for (Map.Entry<String, String> sourceSet : getSourceSets().get().entrySet()) {
            List<String> toAdd = projectAdvice.stream().filter(a ->
                    a.getToConfiguration() != null && !RUNTIME_ONLY_CONFIGURATION_NAME.equals(getScope(a.getToConfiguration()).orElse(null))
            ).filter(a ->
                    sourceSet.getKey().equals(sourceSetName(a.getToConfiguration()))
            ).map(a ->
                    declaration(a.getToConfiguration(), a.getCoordinates().getIdentifier())
            ).sorted().collect(Collectors.toList());

            List<String> toRemove = projectAdvice.stream().filter(a ->
                    a.getFromConfiguration() != null
            ).filter(a ->
                    sourceSet.getKey().equals(sourceSetName(a.getFromConfiguration()))
            ).map(a ->
                    declaration(a.getFromConfiguration(), a.getCoordinates().getIdentifier())
            ).sorted().collect(Collectors.toList());

            if (!toAdd.isEmpty() || !toRemove.isEmpty()) {
                if (message.length() > 0) {
                    message.append("\n\n\n");
                }
                message.append(sourceSet.getValue());
            }
            if (!toAdd.isEmpty()) {
                message.append("\n\nPlease add the following requires directives:");
                message.append("\n    ").append(String.join("\n    ", toAdd));
            }
            if (!toRemove.isEmpty()) {
                message.append("\n\nPlease remove the following requires directives:");
                message.append("\n    ").append(String.join("\n    ", toRemove));
            }
        }
        if (message.length() > 0) {
            throw new RuntimeException(message.toString());
        }
    }

    private String declaration(String conf, String coordinates) {
        return directive(conf) + " " + javaModuleDependencies.moduleName(coordinates).getOrElse(coordinates);
    }

    private String sourceSetName(String configurationName) {
        Optional<String> scope = getScope(configurationName);
        if (!scope.isPresent()) {
            return null;
        }
        String sourceSet = configurationName.substring(0, configurationName.length() - scope.get().length());
        return sourceSet.isEmpty() ? "main" : sourceSet;
    }

    private String directive(String configurationName) {
        return getScope(configurationName).map(SCOPES_TO_DIRECTIVES::get).orElse(null);
    }

    private Optional<String> getScope(String configurationName) {
        return SCOPES_TO_DIRECTIVES.keySet().stream().filter(k -> configurationName.toLowerCase(Locale.ROOT).endsWith(k.toLowerCase(Locale.ROOT))).findFirst();
    }

}
