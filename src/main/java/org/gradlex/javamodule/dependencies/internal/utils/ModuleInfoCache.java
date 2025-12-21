// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.internal.utils;

import static org.gradlex.javamodule.dependencies.internal.utils.ModuleNamingUtil.sourceSetToCapabilitySuffix;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.SourceSet;
import org.gradlex.javamodule.dependencies.LocalModule;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

public abstract class ModuleInfoCache {
    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(ModuleInfoCache.class);

    private final boolean initializedInSettings;
    private final Map<File, ModuleInfo> moduleInfo = new HashMap<>();
    private final Map<String, LocalModule> localModules = new HashMap<>();

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
    public ModuleInfo put(
            File projectRoot,
            String moduleInfoPath,
            String projectPath,
            String artifact,
            Provider<String> group,
            ProviderFactory providers) {
        File folder = new File(projectRoot, moduleInfoPath);
        if (maybePutModuleInfo(folder, providers)) {
            ModuleInfo thisModuleInfo = moduleInfo.get(folder);
            String moduleName = thisModuleInfo.getModuleName();
            String capability = null;
            Path parentDirectory = Paths.get(moduleInfoPath).getParent();
            String capabilitySuffix = parentDirectory == null
                    ? null
                    : sourceSetToCapabilitySuffix(parentDirectory.getFileName().toString());
            if (capabilitySuffix != null) {
                if (group.isPresent()) {
                    capability = group.get() + ":" + artifact + "-" + capabilitySuffix;
                } else {
                    LOGGER.lifecycle("[WARN] [Java Module Dependencies] " + moduleName + " - 'group' not defined!");
                }
            }
            localModules.put(moduleName, new LocalModule(moduleName, projectPath, capability));
            return thisModuleInfo;
        }
        return ModuleInfo.EMPTY;
    }

    public @Nullable LocalModule getLocalModule(String moduleName) {
        return localModules.get(moduleName);
    }

    public Collection<LocalModule> getAllLocalModules() {
        return localModules.values();
    }

    private boolean maybePutModuleInfo(File folder, ProviderFactory providers) {
        if (moduleInfo.containsKey(folder)) {
            return true;
        }
        Provider<ModuleInfo> moduleInfoProvider = provideModuleInfo(folder, providers);
        if (moduleInfoProvider.isPresent()) {
            moduleInfo.put(folder, moduleInfoProvider.get());
            return true;
        }
        return false;
    }

    private Provider<ModuleInfo> provideModuleInfo(File folder, ProviderFactory providers) {
        return providers.of(
                ValueSourceModuleInfo.class,
                spec -> spec.parameters(param -> param.getDir().set(folder)));
    }
}
