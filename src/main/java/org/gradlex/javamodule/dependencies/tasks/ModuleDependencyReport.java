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

package org.gradlex.javamodule.dependencies.tasks;

import org.gradle.api.artifacts.ArtifactCollection;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.diagnostics.DependencyReportTask;
import org.gradlex.javamodule.dependencies.internal.diagnostics.AsciiModuleDependencyReportRenderer;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Set;

public abstract class ModuleDependencyReport extends DependencyReportTask {

    @Internal
    public abstract MapProperty<String, ArtifactCollection> getModuleArtifacts();

    /**
     * Required to track all Jar files as input of the task.
     * Although they are only accessed through getModuleArtifacts().
     */
    @Classpath
    public abstract ConfigurableFileCollection getModulePath();

    @Inject
    protected abstract ProviderFactory getProviders();

    public ModuleDependencyReport() {
        setRenderer(new AsciiModuleDependencyReportRenderer(getModuleArtifacts()));
    }

    @Override
    public void setConfiguration(String configurationName) {
        super.setConfiguration(configurationName);
        configurationsChanged();
    }

    @Override
    public void setConfigurations(Set<Configuration> configurations) {
        super.setConfigurations(configurations);
        configurationsChanged();
    }

    private void configurationsChanged() {
        getModulePath().setFrom();
        getModuleArtifacts().set(Collections.emptyMap());
        for (Configuration conf : getConfigurations()) {
            getModulePath().from(conf);
            getModuleArtifacts().put(conf.getName(), getProviders().provider(() -> conf.getIncoming().getArtifacts()));
        }
    }
}
