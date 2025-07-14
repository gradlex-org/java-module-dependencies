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

import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.SourceSet;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.gradlex.javamodule.dependencies.internal.utils.ModuleNamingUtil.sourceSetToCapabilitySuffix;

public abstract class ModuleInfoCache {
    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(ModuleInfoCache.class);

    private final boolean initializedInSettings;
    private final Map<File, ModuleInfo> moduleInfo = new HashMap<>();
    private final Map<String, String> moduleNameToProjectPath = new HashMap<>();
    private final Map<String, String> moduleNameToCapability = new HashMap<>();

    @Inject
    public ModuleInfoCache(boolean initializedInSettings) {
        this.initializedInSettings = initializedInSettings;
    }

    public boolean isInitializedInSettings() {
        return initializedInSettings;
    }

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

    public @Nullable File getFolder(SourceSet sourceSet, ProviderFactory providers) {
        for (File folder : sourceSet.getJava().getSrcDirs()) {
            if (maybePutModuleInfo(folder, providers)) {
                return folder;
            }
        }
        return null;
    }

    /**
     * @param projectRoot the project that should hold a Java module
     * @return parsed module-info.java for the given project assuming a standard Java project layout
     */
    public ModuleInfo put(File projectRoot, String moduleInfoPath, String projectPath, String artifact, Provider<String> group, ProviderFactory providers) {
        File folder = new File(projectRoot, moduleInfoPath);
        if (maybePutModuleInfo(folder, providers)) {
            ModuleInfo thisModuleInfo = moduleInfo.get(folder);
            moduleNameToProjectPath.put(thisModuleInfo.getModuleName(), projectPath);
            Path parentDirectory = Paths.get(moduleInfoPath).getParent();
            String capabilitySuffix = parentDirectory == null ? null : sourceSetToCapabilitySuffix(parentDirectory.getFileName().toString());
            if (capabilitySuffix != null) {
                if (group.isPresent()) {
                    moduleNameToCapability.put(thisModuleInfo.getModuleName(), group.get() + ":" + artifact + "-" + capabilitySuffix);
                } else {
                    LOGGER.lifecycle(
                            "[WARN] [Java Module Dependencies] " + thisModuleInfo.getModuleName() + " - 'group' not defined!");
                }
            }
            return thisModuleInfo;
        }
        return ModuleInfo.EMPTY;
    }

    public @Nullable String getProjectPath(String moduleName) {
        return moduleNameToProjectPath.get(moduleName);
    }

    public @Nullable String getCapability(String moduleName) {
        return moduleNameToCapability.get(moduleName);
    }

    private boolean maybePutModuleInfo(File folder, ProviderFactory providers) {
        Provider<ModuleInfo> moduleInfoProvider = provideModuleInfo(folder, providers);
        if (moduleInfoProvider.isPresent()) {
            if (!moduleInfo.containsKey(folder)) {
                moduleInfo.put(folder, moduleInfoProvider.get());
            }
            return true;
        }
        return false;
    }

    private Provider<ModuleInfo> provideModuleInfo(File folder, ProviderFactory providers) {
        return providers.of(ValueSourceModuleInfo.class, spec -> spec.parameters(param -> param.getDir().set(folder)));
    }
}
