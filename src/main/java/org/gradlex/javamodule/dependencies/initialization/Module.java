package org.gradlex.javamodule.dependencies.initialization;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Module {
    public abstract Property<String> getFolder();
    public abstract Property<String> getArtifact();
    public abstract Property<String> getGroup();
    public abstract ListProperty<String> getModuleInfoPaths();
    public abstract ListProperty<String> getPlugins();

    @Inject
    public Module(File root) {
        getArtifact().convention(getFolder().map(f -> Paths.get(f).getFileName().toString()));
        getModuleInfoPaths().convention(getFolder().map(projectDir -> listChildren(root, projectDir + "/src")
                .map(srcDir -> new File(srcDir, "java/module-info.java"))
                .filter(File::exists)
                .map(moduleInfo -> "src/" + moduleInfo.getParentFile().getParentFile().getName() + "/java")
                .collect(Collectors.toList())));
    }

    private Stream<File> listChildren(File root, String projectDir) {
        File[] children = new File(root, projectDir).listFiles();
        return children == null ? Stream.empty() : Arrays.stream(children);
    }

    public void plugin(String id) {
        getPlugins().add(id);
    }
}
