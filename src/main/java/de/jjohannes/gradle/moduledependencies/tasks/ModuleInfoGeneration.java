package de.jjohannes.gradle.moduledependencies.tasks;

import de.jjohannes.gradle.moduledependencies.ModuleInfo;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static de.jjohannes.gradle.moduledependencies.ModuleInfo.Directive.*;

public abstract class ModuleInfoGeneration extends DefaultTask {

    @Input
    public abstract Property<String> getModuleName();

    @Input
    public abstract ListProperty<String> getApiDependencies();

    @Input
    public abstract ListProperty<String> getImplementationDependencies();

    @Input
    public abstract ListProperty<String> getCompileOnlyApiDependencies();

    @Input
    public abstract ListProperty<String> getCompileOnlyDependencies();

    @Input
    public abstract MapProperty<String, String> getModuleNameToGA();

    @OutputFile
    public abstract RegularFileProperty getModuleInfoFile();

    @TaskAction
    public void generate() throws IOException {
        File moduleInfo = getModuleInfoFile().get().getAsFile();
        if (moduleInfo.exists()) {
            getLogger().lifecycle("Skipping generation of existing file: " + moduleInfo.getPath());
            return;
        }

        List<String> content = new ArrayList<>();

        content.add("module "+ getModuleName().get() + " {");
        if (!getApiDependencies().get().isEmpty()) {
            content.addAll(dependenciesToModuleDirectives(getApiDependencies().get(), REQUIRES_TRANSITIVE));
            content.add("");
        }
        if (!getImplementationDependencies().get().isEmpty()) {
            content.addAll(dependenciesToModuleDirectives(getImplementationDependencies().get(), REQUIRES));
            content.add("");
        }
        if (!getCompileOnlyApiDependencies().get().isEmpty()) {
            content.addAll(dependenciesToModuleDirectives(getCompileOnlyApiDependencies().get(), REQUIRES_STATIC_TRANSITIVE));
            content.add("");
        }
        if (!getCompileOnlyDependencies().get().isEmpty()) {
            content.addAll(dependenciesToModuleDirectives(getCompileOnlyDependencies().get(), REQUIRES_STATIC));
            content.add("");
        }
        content.add("}");

        Files.write(moduleInfo.toPath(), content);
    }

    private Collection<String> dependenciesToModuleDirectives(List<String> dependencies, ModuleInfo.Directive directive) {
        return dependencies.stream().map(gaOrProjectModuleName -> {
            String moduleName = moduleName(gaOrProjectModuleName);
            if (moduleName == null) {
                getLogger().lifecycle("Skipping '" + gaOrProjectModuleName + "' - no mapping - run ':analyzeModulePath' for more details");
                return "    // " + directive.literal() + " " + gaOrProjectModuleName + ";";
            } else {
                return "    " + directive.literal() + " " + moduleName + ";";
            }
        }).collect(Collectors.toList());
    }

    private String moduleName(String gaOrProjectModuleName) {
        if (!gaOrProjectModuleName.contains(":")) {
            return gaOrProjectModuleName;
        }
        return getModuleNameToGA().get().get(gaOrProjectModuleName);
    }
}
