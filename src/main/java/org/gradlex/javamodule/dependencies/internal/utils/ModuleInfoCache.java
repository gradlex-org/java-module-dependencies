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

package org.gradlex.javamodule.dependencies.internal.utils;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.SourceSet;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.gradlex.javamodule.dependencies.internal.utils.ModuleNamingUtil.sourceSetToCapabilitySuffix;

public abstract class ModuleInfoCache {

    private final Map<File, ModuleInfo> moduleInfo = new HashMap<>();
    private final Map<String, String> moduleNameToProjectPath = new HashMap<>();
    private final Map<String, String> moduleNameToCapability = new HashMap<>();

    @Inject
    public abstract ObjectFactory getObjects();

    /**
     * Returns the module-info.java for the given SourceSet. If the SourceSet has multiple source folders with multiple
     * module-info files (which is usually a broken setup) the first file found is returned.
     *
     * @param sourceSet the SourceSet representing a module
     * @return parsed module-info.java for the given SourceSet
     */
    public ModuleInfo get(SourceSet sourceSet, ProviderFactory providers) {
        for (File folder : sourceSet.getJava().getSrcDirs()) {
            if (maybePutModuleInfo(folder, providers)) {
                return moduleInfo.get(folder);
            }
        }
        return ModuleInfo.EMPTY;
    }

    /**
     * @param projectRoot the project that should hold a Java module
     * @return parsed module-info.java for the given project assuming a standard Java project layout
     */
    public ModuleInfo put(File projectRoot, String moduleInfoPath, String artifact, Provider<String> group, ProviderFactory providers) {
        File folder = new File(projectRoot, moduleInfoPath);
        if (maybePutModuleInfo(folder, providers)) {
            ModuleInfo thisModuleInfo = moduleInfo.get(folder);
            moduleNameToProjectPath.put(thisModuleInfo.getModuleName(), ":" + artifact);
            String capabilitySuffix = sourceSetToCapabilitySuffix(Paths.get(moduleInfoPath).getFileName().toString());
            if (group.isPresent() && capabilitySuffix != null) {
                moduleNameToCapability.put(thisModuleInfo.getModuleName(), group.get() + ":" + artifact + "-" + capabilitySuffix);
            }
            return thisModuleInfo;
        }
        return ModuleInfo.EMPTY;
    }

    public String getProjectPath(String moduleName) {
        return moduleNameToProjectPath.get(moduleName);
    }

    public String getCapability(String moduleName) {
        return moduleNameToCapability.get(moduleName);
    }

    private boolean maybePutModuleInfo(File folder, ProviderFactory providers) {
        RegularFileProperty moduleInfoFile = getObjects().fileProperty();
        moduleInfoFile.set(new File(folder, "module-info.java"));
        Provider<String> moduleInfoContent = providers.fileContents(moduleInfoFile).getAsText();
        if (moduleInfoContent.isPresent()) {
            if (!moduleInfo.containsKey(folder)) {
                moduleInfo.put(folder, new ModuleInfo(moduleInfoContent.get(), moduleInfoFile.get().getAsFile()));
            }
            return true;
        }
        return false;
    }

    public boolean initializedInSettings() {
        return false;
    }
}
