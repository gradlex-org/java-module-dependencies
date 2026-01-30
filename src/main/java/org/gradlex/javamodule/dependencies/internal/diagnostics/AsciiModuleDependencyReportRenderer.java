// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.internal.diagnostics;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Map;
import org.gradle.api.artifacts.ArtifactCollection;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.diagnostics.internal.ConfigurationDetails;
import org.gradle.api.tasks.diagnostics.internal.ProjectDetails;
import org.gradle.api.tasks.diagnostics.internal.dependencies.AsciiDependencyReportRenderer;
import org.gradle.api.tasks.diagnostics.internal.graph.DependencyGraphsRenderer;
import org.gradle.api.tasks.diagnostics.internal.graph.NodeRenderer;
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableDependency;
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableModuleResult;
import org.gradle.internal.graph.GraphRenderer;
import org.gradle.internal.logging.text.StyledTextOutput;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class AsciiModuleDependencyReportRenderer extends AsciiDependencyReportRenderer {

    private @Nullable DependencyGraphsRenderer dependencyGraphRenderer;
    private final Provider<Map<String, ArtifactCollection>> resolvedJars;

    public AsciiModuleDependencyReportRenderer(Provider<Map<String, ArtifactCollection>> resolvedJars) {
        this.resolvedJars = resolvedJars;
    }

    @Override
    public void startProject(ProjectDetails project) {
        super.startProject(project);
        GraphRenderer renderer = new GraphRenderer(this.getTextOutput());
        this.dependencyGraphRenderer = new DependencyGraphsRenderer(
                this.getTextOutput(), renderer, NodeRenderer.NO_OP, new StyledNodeRenderer());
    }

    @Override
    public void render(ConfigurationDetails configuration) {
        if (configuration.isCanBeResolved()) {
            ResolvedComponentResult result =
                    requireNonNull(configuration.getResolutionResultRoot()).get();
            RenderableModuleResult root = new RenderableJavaModuleResult(
                    result, resolvedJars.get().get(configuration.getName()).getArtifacts());
            renderNow(root);
        } else {
            renderNow(requireNonNull(configuration.getUnresolvableResult()));
        }
    }

    private void renderNow(RenderableDependency root) {
        if (root.getChildren().isEmpty()) {
            this.getTextOutput().withStyle(StyledTextOutput.Style.Info).text("No dependencies");
            this.getTextOutput().println();
        } else if (this.dependencyGraphRenderer != null) {
            this.dependencyGraphRenderer.render(Collections.singletonList(root));
        }
    }

    public void complete() {
        if (this.dependencyGraphRenderer != null) {
            this.dependencyGraphRenderer.complete();
        }
        super.complete();
    }
}
