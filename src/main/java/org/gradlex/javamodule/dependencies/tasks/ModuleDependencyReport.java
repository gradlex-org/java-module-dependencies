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

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.ArtifactCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.diagnostics.DependencyReportTask;
import org.gradlex.javamodule.dependencies.internal.diagnostics.AsciiModuleDependencyReportRenderer;

@NonNullApi
public abstract class ModuleDependencyReport extends DependencyReportTask {

    private boolean configurationSetByUser = false;

    @Internal
    public abstract ListProperty<ArtifactCollection> getModuleArtifacts();

    public ModuleDependencyReport() {
        setRenderer(new AsciiModuleDependencyReportRenderer(getModuleArtifacts()));
    }

    @Override
    public void setConfiguration(String configurationName) {
        super.setConfiguration(configurationName);
        configurationSetByUser = true;
    }

    @Internal
    public boolean isConfigurationSetByUser() {
        return configurationSetByUser;
    }
}
