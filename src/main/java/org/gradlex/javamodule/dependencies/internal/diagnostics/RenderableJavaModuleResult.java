// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.internal.diagnostics;

import java.util.LinkedHashSet;
import java.util.Set;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.artifacts.result.UnresolvedDependencyResult;
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableDependency;
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableModuleResult;
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableUnresolvedDependencyResult;

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
