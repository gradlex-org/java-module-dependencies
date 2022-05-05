package de.jjohannes.gradle.moduledependencies.bridges;

import de.jjohannes.gradle.javamodules.ExtraModuleInfoPluginExtension;
import de.jjohannes.gradle.moduledependencies.JavaModuleDependenciesExtension;
import org.gradle.api.Project;

import java.util.Map;
import java.util.stream.Collectors;

public class ExtraJavaModuleInfoBridge {

    public static void autoRegisterPatchedModuleMappings(Project project, JavaModuleDependenciesExtension javaModuleDependencies) {
        ExtraModuleInfoPluginExtension extraModuleInfo = project.getExtensions().getByType(ExtraModuleInfoPluginExtension.class);
        javaModuleDependencies.getModuleNameToGA().putAll(extraModuleInfo.getModuleSpecs().map(
                moduleSpecs -> moduleSpecs.entrySet().stream().collect(Collectors.toMap(e -> e.getValue().getModuleName(), Map.Entry::getKey))));
    }
}
