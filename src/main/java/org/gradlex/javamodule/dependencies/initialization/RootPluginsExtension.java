// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.initialization;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.IsolatedAction;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class RootPluginsExtension {

    private final List<String> ids = new ArrayList<>();

    @Inject
    public RootPluginsExtension(Settings settings) {
        settings.getGradle().getLifecycle().beforeProject(new ApplyPluginAction(ids));
    }

    public void id(String id) {
        ids.add(id);
    }

    private static class ApplyPluginAction implements IsolatedAction<Project> {

        private final List<String> ids;

        public ApplyPluginAction(List<String> ids) {
            this.ids = ids;
        }

        @Override
        public void execute(Project project) {
            if (isRoot(project)) {
                for (String id : ids) {
                    project.getPlugins().apply(id);
                }
            }
        }

        private boolean isRoot(Project project) {
            return project == project.getRootProject();
        }
    }
}
