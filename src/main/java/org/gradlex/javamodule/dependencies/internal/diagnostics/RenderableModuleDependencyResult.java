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

package org.gradlex.javamodule.dependencies.internal.diagnostics;

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ComponentSelector;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.artifacts.result.UnresolvedDependencyResult;
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableDependency;
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableDependencyResult;
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableUnresolvedDependencyResult;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.gradlex.javamodule.dependencies.internal.utils.ModuleJar.isRealModule;
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleJar.readModuleNameFromJarFile;

@NonNullApi
public class RenderableModuleDependencyResult extends RenderableDependencyResult {
    private final ResolvedDependencyResult dependency;
    private final Set<ResolvedArtifactResult> resolvedJars;

    public RenderableModuleDependencyResult(ResolvedDependencyResult dependency, Set<ResolvedArtifactResult> resolvedJars) {
        super(dependency);
        this.dependency = dependency;
        this.resolvedJars = resolvedJars;
    }

    @Override
    public Set<RenderableDependency> getChildren() {
        Set<RenderableDependency> out = new LinkedHashSet<>();
        for (DependencyResult d : dependency.getSelected().getDependencies()) {
            if (d instanceof UnresolvedDependencyResult) {
                out.add(new RenderableUnresolvedDependencyResult((UnresolvedDependencyResult) d));
            } else {
                ResolvedDependencyResult resolved = (ResolvedDependencyResult) d;
                ResolvedArtifactResult artifact = resolvedJars.stream().filter(a ->
                        a.getId().getComponentIdentifier().equals(resolved.getSelected().getId())).findFirst().orElse(null);
                if (artifact != null) {
                    out.add(new RenderableModuleDependencyResult(resolved, resolvedJars));
                }
            }
        }
        return out;
    }

    @Override
    public String getName() {
        ComponentSelector requested = getRequested();
        ComponentIdentifier selected = getActual();
        ResolvedArtifactResult artifact = resolvedJars.stream().filter(a ->
                a.getId().getComponentIdentifier().equals(selected)).findFirst().orElse(null);

        try {
            if (artifact == null) {
                return "[BOM] " + selected.getDisplayName();
            } else {
                String actualModuleName = readModuleNameFromJarFile(artifact.getFile());
                if (actualModuleName == null) {
                    return "[CLASSPATH] " + selected.getDisplayName();
                } else {
                    String version = "";
                    String coordinates =  selected.getDisplayName();
                    String jarName = artifact.getFile().getName();
                    if (selected instanceof ModuleComponentIdentifier) {
                        String selectedVersion = ((ModuleComponentIdentifier) selected).getVersion();
                        version = " (" + selectedVersion + ")";
                        if (requested instanceof ModuleComponentSelector) {
                            String requestedVersion = ((ModuleComponentSelector) requested).getVersion();
                            if (!requestedVersion.isEmpty() && !selectedVersion.equals(requestedVersion)) {
                                version = " (" + requestedVersion + " -> " + selectedVersion + ")";
                            }
                        }
                        coordinates = ((ModuleComponentIdentifier) selected).getModuleIdentifier().toString();
                    }
                    String auto = isRealModule(artifact.getFile()) ? "" : "[AUTO] ";
                    return auto + actualModuleName + version + " | " + coordinates +
                            (isConstraint() ? "" : " | " + jarName);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isConstraint() {
        return getResolutionState() == ResolutionState.RESOLVED_CONSTRAINT;
    }
}
