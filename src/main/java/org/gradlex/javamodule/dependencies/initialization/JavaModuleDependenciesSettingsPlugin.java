package org.gradlex.javamodule.dependencies.initialization;

import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;

@NonNullApi
public abstract class JavaModuleDependenciesSettingsPlugin implements Plugin<Settings> {

    @Override
    public void apply(Settings settings) {
        registerExtension(settings);
    }

    private void registerExtension(Settings settings) {
        settings.getExtensions().create("rootPlugins", RootPluginsExtension.class, settings);
        settings.getExtensions().create("javaModules", JavaModulesExtension.class, settings);
    }
}
