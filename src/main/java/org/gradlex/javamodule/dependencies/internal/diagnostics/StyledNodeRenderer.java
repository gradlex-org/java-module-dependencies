// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.internal.diagnostics;

import static org.gradle.internal.logging.text.StyledTextOutput.Style.Description;
import static org.gradle.internal.logging.text.StyledTextOutput.Style.Failure;
import static org.gradle.internal.logging.text.StyledTextOutput.Style.Info;

import org.gradle.api.tasks.diagnostics.internal.graph.NodeRenderer;
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableDependency;
import org.gradle.internal.logging.text.StyledTextOutput;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class StyledNodeRenderer implements NodeRenderer {

    @Override
    public void renderNode(StyledTextOutput output, RenderableDependency node, boolean alreadyRendered) {
        String name = node.getName();
        if (name.startsWith("[BOM]")) {
            output.withStyle(Description).text(name);
        } else if (name.startsWith("[AUTO]")) {
            output.withStyle(Failure).text(name);
        } else if (name.startsWith("[CLASSPATH]")) {
            output.withStyle(Failure).text(name);
        } else {
            int idx = name.indexOf('|');
            output.text(name.substring(0, idx)).withStyle(Description).text(name.substring(idx));
        }
        switch (node.getResolutionState()) {
            case FAILED:
                output.withStyle(Failure).text(" FAILED");
                break;
            case RESOLVED:
                if (alreadyRendered && !node.getChildren().isEmpty()) {
                    output.withStyle(Info).text(" (*)");
                }
                break;
            case RESOLVED_CONSTRAINT:
                output.withStyle(Info).text(" (c)");
                break;
            case UNRESOLVED:
                output.withStyle(Info).text(" (n)");
                break;
        }
    }
}
