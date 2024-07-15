package org.gradlex.javamodule.dependencies.initialization;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import java.nio.file.Paths;

public abstract class Module {
    public abstract Property<String> getFolder();
    public abstract Property<String> getArtifact();
    public abstract Property<String> getGroup();
    public abstract Property<String> getModuleInfoPath();
    public abstract ListProperty<String> getPlugins();

    public Module() {
        getArtifact().convention(getFolder().map(f -> Paths.get(f).getFileName().toString()));
        getModuleInfoPath().convention("src/main/java");
    }

    public void plugin(String id) {
        getPlugins().add(id);
    }


}
