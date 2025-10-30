// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.tasks;

import java.util.Collections;
import java.util.Set;
import javax.inject.Inject;
import org.gradle.api.artifacts.ArtifactCollection;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.diagnostics.DependencyReportTask;
import org.gradlex.javamodule.dependencies.internal.diagnostics.AsciiModuleDependencyReportRenderer;

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
            getModuleArtifacts().put(conf.getName(), getProviders().provider(() -> conf.getIncoming()
                    .getArtifacts()));
        }
    }
}
