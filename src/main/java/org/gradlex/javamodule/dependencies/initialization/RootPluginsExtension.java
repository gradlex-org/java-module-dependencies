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

import static org.gradlex.javamodule.dependencies.initialization.JavaModulesExtension.SUPPORT_PROJECT_ISOLATION;

public abstract class RootPluginsExtension {

    private final Settings settings;

    @Inject
    public RootPluginsExtension(Settings settings) {
        this.settings = settings;
    }

    public void id(String id) {
        if (SUPPORT_PROJECT_ISOLATION) {
            settings.getGradle().getLifecycle().beforeProject(new ApplyPluginAction(id));
        } else {
            settings.getGradle().beforeProject(new ApplyPluginAction(id));
        }
    }

    @NonNullApi
    private static class ApplyPluginAction implements IsolatedAction<Project>, Action<Project> {

        private final String id;

        public ApplyPluginAction(String id) {
            this.id = id;
        }

        @Override
        public void execute(Project project) {
            if (isRoot(project)) {
                project.getPlugins().apply(id);
            }
        }

        private boolean isRoot(Project project) {
            return project == project.getRootProject();
        }
    }
}
