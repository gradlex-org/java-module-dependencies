/*
 * Copyright 2022 the GradleX team.
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

package org.gradlex.javamodule.dependencies;

import org.gradle.api.Task;
import org.gradle.api.artifacts.VersionCatalog;
import org.gradle.api.artifacts.VersionCatalogsExtension;
import org.gradle.api.artifacts.VersionConstraint;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradlex.javamodule.dependencies.internal.compile.AddSyntheticModulesToCompileClasspathAction;
import org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo;
import org.gradlex.javamodule.dependencies.internal.utils.ModuleInfoCache;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.empty;

/**
 * - Configure behavior of the 'java-module-dependencies' plugin
 * - Add additional mappings using {@link #getModuleNameToGA()}
 * - Define dependencies and dependency constraints by Module Name
 *   using {@link #ga(String)}, {@link #gav(String, String)} or {@link #gav(String)}
 */
public abstract class JavaModuleDependenciesExtension {
    static final String JAVA_MODULE_DEPENDENCIES = "javaModuleDependencies";

    private final VersionCatalogsExtension versionCatalogs;
    private final ModuleInfoCache moduleInfoCache;

    /**
     * @return the mappings from Module Name to GA coordinates; can be modified
     */
    public abstract MapProperty<String, String> getModuleNameToGA();

    /**
     * If your Module Names all start with a common prefix (e.g. 'com.example.product.module.') followed by a
     * name that corresponds to the artifact name and all have the group (e.g. 'com.example.product'), you can
     * register a mapping for all these Modules. and with that allow Gradle to map them correctly even if you
     * publish some of your Modules or use included builds.
     *
     * moduleNamePrefixToGroup.put("com.example.product.module.", "com.example.product")
     *
     * @return the mappings from 'Module Name Prefix' to 'group'
     */
    public abstract MapProperty<String, String> getModuleNamePrefixToGroup();

    /**
     * @return If a Version Catalog is used: print a WARN for missing versions (default is 'true')
     */
    public abstract Property<Boolean> getWarnForMissingVersions();

    /**
     * @return If a Version Catalog is used: which catalog? (default is 'libs')
     */
    public abstract Property<String> getVersionCatalogName();

    public JavaModuleDependenciesExtension(VersionCatalogsExtension versionCatalogs) {
        this.versionCatalogs = versionCatalogs;
        this.moduleInfoCache = getObjects().newInstance(ModuleInfoCache.class);
        getWarnForMissingVersions().convention(versionCatalogs != null);
        getVersionCatalogName().convention("libs");
        getModuleNameToGA().putAll(SharedMappings.mappings);
    }

    /**
     * Converts 'Module Name' to GA coordinates that can be used in
     * dependency declarations as String: "group:name"
     *
     * @param moduleName The Module Name
     * @return Dependency notation
     */
    public Provider<String> ga(String moduleName) {
        return getModuleNameToGA().getting(moduleName);
    }

    /**
     * Converts 'Module Name' and 'Version' to GAV coordinates that can be used in
     * dependency declarations as String: "group:name:version"
     *
     * @param moduleName The Module Name
     * @param version The (required) version
     * @return Dependency notation
     */
    public Provider<String> gav(String moduleName, String version) {
        return getModuleNameToGA().getting(moduleName).map(s -> s + ":" + version);
    }

    /**
     * If a Version Catalog is used:
     * Converts 'Module Name' and the matching 'Version' from the Version Catalog to
     * GAV coordinates that can be used in dependency Declarations as Map:
     * [group: "...", name: "...", version: "..."]
     *
     * @param moduleName The Module Name
     * @return Dependency notation
     */
    public Provider<Map<String, Object>> gav(String moduleName) {
        Provider<String> ga = ga(moduleName);

        VersionCatalog catalog = null;
        if (versionCatalogs != null) {
            String catalogName = getVersionCatalogName().get();
            catalog = versionCatalogs.named(catalogName);
        }
        Optional<VersionConstraint> version = catalog == null ? empty() : catalog.findVersion(moduleName.replace('_', '.'));

        return ga.map(s -> {
            Map<String, Object> gav = new HashMap<>();
            String[] gaSplit = s.split(":");
            gav.put(GAV.GROUP, gaSplit[0]);
            gav.put(GAV.ARTIFACT, gaSplit[1]);
            version.ifPresent(versionConstraint -> gav.put(GAV.VERSION, versionConstraint));
            return gav;
        });
    }

    /**
     * Finds the Module Name for given coordinates
     *
     * @param ga The GA coordinates
     * @return the first name found or 'null'
     */
    @Nullable
    public String moduleName(String ga) {
        for(Map.Entry<String, String> mapping: getModuleNameToGA().get().entrySet()) {
            if (mapping.getValue().equals(ga)) {
                return mapping.getKey();
            }
        }
        return null;
    }

    /**
     * Adds support for compiling module-info.java in the given source set with the given task,
     * if 'requires runtime' dependencies are used.
     *
     * @param task      The task that compiles code from the given source set
     * @param sourceSet The source set that contains the module-info.java
     * @return collection of folders containing synthetic module-info.class files
     */
    public FileCollection addRequiresRuntimeSupport(JavaCompile task, SourceSet sourceSet) {
        return doAddRequiresRuntimeSupport(task, sourceSet);
    }

    /**
     * Adds support for generating Javadoc for the module-info.java in the given source set with the given task,
     * if 'requires runtime' dependencies are used.
     *
     * @param task      The task that generates Javadoc from the given source set
     * @param sourceSet The source set that contains the module-info.java
     * @return collection of folders containing synthetic module-info.class files
     */
    public FileCollection addRequiresRuntimeSupport(Javadoc task, SourceSet sourceSet) {
        return doAddRequiresRuntimeSupport(task, sourceSet);
    }

    FileCollection doAddRequiresRuntimeSupport(Task task, SourceSet sourceSet) {
        List<String> requiresRuntime = getModuleInfoCache().get(sourceSet).get(ModuleInfo.Directive.REQUIRES_RUNTIME);
        ConfigurableFileCollection syntheticModuleInfoFolders = getObjects().fileCollection();
        if (!requiresRuntime.isEmpty()) {
            Provider<Directory> tmpDir = getLayout().getBuildDirectory().dir("tmp/java-module-dependencies/" + task.getName());
            requiresRuntime.forEach(moduleName -> syntheticModuleInfoFolders.from(tmpDir.map(dir -> dir.dir(moduleName))));
            task.doFirst(getObjects().newInstance(AddSyntheticModulesToCompileClasspathAction.class, syntheticModuleInfoFolders));
            return syntheticModuleInfoFolders;
        }
        return syntheticModuleInfoFolders;
    }

    @Inject
    protected abstract ObjectFactory getObjects();

    @Inject
    protected abstract ProjectLayout getLayout();

    ModuleInfoCache getModuleInfoCache() {
        return moduleInfoCache;
    }
}
