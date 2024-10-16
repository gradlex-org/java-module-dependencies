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

package org.gradlex.javamodule.dependencies.initialization;

import org.gradle.api.Action;
import org.gradle.api.IsolatedAction;
import org.gradle.api.NonNullApi;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public abstract class RootPluginsExtension {

    private final List<String> ids = new ArrayList<>();

    @Inject
    public RootPluginsExtension(Settings settings) {
        settings.getGradle().getLifecycle().beforeProject(new ApplyPluginAction(ids));
    }

    public void id(String id) {
        ids.add(id);
    }

    @NonNullApi
    private static class ApplyPluginAction implements IsolatedAction<Project>, Action<Project> {

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
