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
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.artifacts.result.UnresolvedDependencyResult;
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableDependency;
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableModuleResult;
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableUnresolvedDependencyResult;

import java.util.LinkedHashSet;
import java.util.Set;

public class RenderableJavaModuleResult extends RenderableModuleResult {

    private final Set<ResolvedArtifactResult> resolvedJars;

    public RenderableJavaModuleResult(ResolvedComponentResult module, Set<ResolvedArtifactResult> resolvedJars) {
        super(module);
        this.resolvedJars = resolvedJars;
    }

    @Override
    public Set<RenderableDependency> getChildren() {
        Set<RenderableDependency> out = new LinkedHashSet<>();
        for (DependencyResult d : module.getDependencies()) {
            if (d instanceof UnresolvedDependencyResult) {
                out.add(new RenderableUnresolvedDependencyResult((UnresolvedDependencyResult) d));
            } else {
                out.add(new RenderableModuleDependencyResult((ResolvedDependencyResult) d, resolvedJars));
            }
        }
        return out;
    }
}
