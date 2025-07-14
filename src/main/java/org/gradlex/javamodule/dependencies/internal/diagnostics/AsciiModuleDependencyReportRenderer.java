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
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

import static java.util.Objects.requireNonNull;

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
        this.dependencyGraphRenderer = new DependencyGraphsRenderer(this.getTextOutput(), renderer, NodeRenderer.NO_OP, new StyledNodeRenderer());
    }

    @Override
    public void render(ConfigurationDetails configuration) {
        if (configuration.isCanBeResolved()) {
            ResolvedComponentResult result = requireNonNull(configuration.getResolutionResultRoot()).get();
            RenderableModuleResult root = new RenderableJavaModuleResult(result, resolvedJars.get().get(configuration.getName()).getArtifacts());
            renderNow(root);
        } else {
            renderNow(requireNonNull(configuration.getUnresolvableResult()));
        }
    }

    private void renderNow(RenderableDependency root) {
        if (root.getChildren().isEmpty()) {
            this.getTextOutput().withStyle(StyledTextOutput.Style.Info).text("No dependencies");
            this.getTextOutput().println();
        } else if (this.dependencyGraphRenderer != null){
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
