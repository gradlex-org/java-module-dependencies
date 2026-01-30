// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.internal.bridges;

import java.util.Map;
import java.util.stream.Collectors;
import org.gradle.api.Project;
import org.gradlex.javamodule.dependencies.JavaModuleDependenciesExtension;
import org.gradlex.javamodule.moduleinfo.ExtraJavaModuleInfoPluginExtension;
import org.gradlex.javamodule.moduleinfo.ModuleSpec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ExtraJavaModuleInfoBridge {

    public static void autoRegisterPatchedModuleMappings(
            Project project, JavaModuleDependenciesExtension javaModuleDependencies) {
        ExtraJavaModuleInfoPluginExtension extraJavaModuleInfo =
                project.getExtensions().getByType(ExtraJavaModuleInfoPluginExtension.class);
        javaModuleDependencies
                .getModuleNameToGA()
                .putAll(extraJavaModuleInfo.getModuleSpecs().map(moduleSpecs -> moduleSpecs.entrySet().stream()
                        .collect(Collectors.toMap(
                                ExtraJavaModuleInfoBridge::moduleNameKey, Map.Entry::getKey, (a, b) -> b))));
    }

    private static String moduleNameKey(Map.Entry<String, ModuleSpec> entry) {
        if (!entry.getKey().contains(":")) {
            // Entry is not usable as mapping (e.g., because it uses file names instead of GA coordinates).
            // Invalidate this mapping entry by creating a key that is not a valid module name.
            return "__" + entry.getValue().getModuleName();
        }
        return entry.getValue().getModuleName();
    }
}
