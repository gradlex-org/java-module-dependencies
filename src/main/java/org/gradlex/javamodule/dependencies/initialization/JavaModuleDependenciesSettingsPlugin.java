// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.initialization;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.util.GradleVersion;

public abstract class JavaModuleDependenciesSettingsPlugin implements Plugin<Settings> {

    @Override
    public void apply(Settings settings) {
        if (GradleVersion.current().compareTo(GradleVersion.version("8.8")) < 0) {
            throw new GradleException("This settings plugin requires Gradle 8.8+");
        }
        registerExtension(settings);
    }

    private void registerExtension(Settings settings) {
        settings.getExtensions().create("rootPlugins", RootPluginsExtension.class, settings);
        settings.getExtensions().create("javaModules", JavaModulesExtension.class, settings);
    }
}
