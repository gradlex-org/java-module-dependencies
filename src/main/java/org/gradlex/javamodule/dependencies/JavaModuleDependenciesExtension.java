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

package org.gradlex.javamodule.dependencies;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.VersionCatalog;
import org.gradle.api.artifacts.VersionCatalogsExtension;
import org.gradle.api.artifacts.VersionConstraint;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradlex.javamodule.dependencies.internal.compile.AddSyntheticModulesToCompileClasspathAction;
import org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo;
import org.gradlex.javamodule.dependencies.internal.utils.ModuleInfoCache;

import javax.inject.Inject;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
     * Register mapping from Module Name to GA Coordinates (and optionally Capability Coordinates).
     * - moduleNameToGA.put("org.slf4j", "org.slf4j:slf4j-api")
     * - moduleNameToGA.put("org.slf4j.test.fixtures", "org.slf4j:slf4j-api|org.slf4j:slf4j-api-test-fixtures")
     *
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

    /**
     * Set this to true to use the analytic help tasks (like :moduleDependencies) of the plugin without performing
     * the actual dependency calculation.
     *
     * @return true – for analyse-only mode
     */
    public abstract Property<Boolean> getAnalyseOnly();

    public JavaModuleDependenciesExtension(VersionCatalogsExtension versionCatalogs) {
        this.versionCatalogs = versionCatalogs;
        this.moduleInfoCache = getObjects().newInstance(ModuleInfoCache.class);
        getWarnForMissingVersions().convention(versionCatalogs != null);
        getVersionCatalogName().convention("libs");
        getAnalyseOnly().convention(false);
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
        return getModuleNameToGA().getting(moduleName).orElse(mapByPrefix(getProviders().provider(() -> moduleName))).orElse(errorIfNotFound(moduleName));
    }

    /**
     * Converts 'Module Name' to GA coordinates that can be used in
     * dependency declarations as String: "group:name"
     *
     * @param moduleName The Module Name
     * @return Dependency notation
     */
    public Provider<String> ga(Provider<String> moduleName) {
        return moduleName.flatMap(n -> getModuleNameToGA().getting(n)).orElse(mapByPrefix(moduleName)).orElse(errorIfNotFound(moduleName));
    }

    private Provider<String> mapByPrefix(Provider<String> moduleName) {
        return getModuleNamePrefixToGroup().map(
                m -> {
                    Optional<Map.Entry<String, String>> prefixToGroup = m.entrySet().stream()
                            .filter(e -> moduleName.get().startsWith(e.getKey())).findFirst();
                    if (prefixToGroup.isPresent()) {
                        String group = prefixToGroup.get().getValue();
                        String artifact = moduleName.get().substring(prefixToGroup.get().getKey().length());
                        return group + ":" + artifact;
                    }
                    return null;
                }
        );
    }

    public Provider<Dependency> create(String moduleName, SourceSet sourceSetWithModuleInfo) {
        return getProviders().provider(() -> {
            Map<String, String> allProjectNamesAndGroups = getProject().getRootProject().getSubprojects().stream().collect(
                    Collectors.toMap(Project::getName, p -> (String) p.getGroup()));

            Provider<String> coordinates = getModuleNameToGA().getting(moduleName).orElse(mapByPrefix(getProviders().provider(() -> moduleName)));

            ModuleInfo moduleInfo = getModuleInfoCache().get(sourceSetWithModuleInfo);
            String ownModuleNamesPrefix = moduleInfo.moduleNamePrefix(getProject().getName(), sourceSetWithModuleInfo.getName());

            String moduleNameSuffix = ownModuleNamesPrefix == null ? null :
                    moduleName.startsWith(ownModuleNamesPrefix + ".") ? moduleName.substring(ownModuleNamesPrefix.length() + 1) :
                            ownModuleNamesPrefix.isEmpty() ? moduleName : null;

            String parentPath = getProject().getParent() == null ? "" : getProject().getParent().getPath();
            Optional<String> perfectMatch = allProjectNamesAndGroups.keySet().stream().filter(p -> p.replace("-", ".").equals(moduleNameSuffix)).findFirst();
            Optional<String> existingProjectName = allProjectNamesAndGroups.keySet().stream().filter(p -> moduleNameSuffix != null && moduleNameSuffix.startsWith(p.replace("-", ".") + "."))
                    .max(Comparator.comparingInt(String::length));

            if (perfectMatch.isPresent()) {
                Dependency projectDependency = getDependencies().create(getProject().project(parentPath + ":" + perfectMatch.get()));
                projectDependency.because(moduleName);
                return projectDependency;
            } else if (existingProjectName.isPresent()) {
                // no exact match -> add capability to point at Module in other source set
                String projectName = existingProjectName.get();
                ProjectDependency projectDependency = (ProjectDependency) getDependencies().create(getProject().project(parentPath + ":" + projectName));
                String capabilityName = projectName + moduleNameSuffix.substring(projectName.length()).replace(".", "-");
                projectDependency.capabilities(c -> c.requireCapabilities(
                        allProjectNamesAndGroups.get(projectName) + ":" + capabilityName));
                projectDependency.because(moduleName);
                return projectDependency;
            } else if (coordinates.isPresent()) {
                Map<String, Object> component;
                String capability;
                if (coordinates.get().contains("|")) {
                    String[] split = coordinates.get().split("\\|");
                    component = findGav(split[0], moduleName);
                    if (split[1].contains(":")) {
                        capability = split[1];
                    } else {
                        // only classifier was specified
                        capability = split[0] + "-" + split[1];
                    }
                } else {
                    component = findGav(coordinates.get(), moduleName);
                    capability = null;
                }
                ModuleDependency dependency = (ModuleDependency) getDependencies().create(component);
                dependency.because(moduleName);
                if (capability != null) {
                    dependency.capabilities(c -> c.requireCapability(capability));
                }
                if (!component.containsKey(GAV.VERSION)) {
                    warnVersionMissing(moduleName, component, moduleInfo.getFilePath());
                }
                return dependency;
            } else {
                getProject().getLogger().lifecycle(
                        "[WARN] [Java Module Dependencies] javaModuleDependencies.moduleNameToGA.put(\"" + moduleName + "\", \"group:artifact\") mapping is missing.");
                return null;
            }
        });
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
        return ga(moduleName).map(s -> s + ":" + version);
    }

    /**
     * Converts 'Module Name' and 'Version' to GAV coordinates that can be used in
     * dependency declarations as String: "group:name:version"
     *
     * @param moduleName The Module Name
     * @param version The (required) version
     * @return Dependency notation
     */
    public Provider<String> gav(Provider<String> moduleName, Provider<String> version) {
        return ga(moduleName).map(s -> s + ":" + version.get());
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
        return ga(moduleName).map(ga -> findGav(ga, moduleName));
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
    public Provider<Map<String, Object>> gav(Provider<String> moduleName) {
        return ga(moduleName).map(ga -> findGav(ga, moduleName.get()));
    }

    private Map<String, Object> findGav(String ga, String moduleName) {
        VersionCatalog catalog = versionCatalogs == null ? null : versionCatalogs.find(getVersionCatalogName().get()).orElse(null);
        Optional<VersionConstraint> version = catalog == null ? empty() : catalog.findVersion(moduleName.replace('_', '.'));
        Map<String, Object> gav = new HashMap<>();
        String[] gaSplit = ga.split(":");
        gav.put(GAV.GROUP, gaSplit[0]);
        gav.put(GAV.ARTIFACT, gaSplit[1]);
        version.ifPresent(versionConstraint -> gav.put(GAV.VERSION, versionConstraint));
        return gav;
    }

    /**
     * Finds the Module Name for given coordinates
     *
     * @param ga The GA coordinates
     * @return the first name found or unset
     */
    public Provider<String> moduleName(String ga) {
        return moduleName(getProviders().provider(() -> ga));
    }

    /**
     * Finds the Module Name for given coordinates
     *
     * @param ga The GA coordinates
     * @return the first name found or unset
     */
    public Provider<String> moduleName(Provider<String> ga) {
        return ga.map(groupArtifact -> {
            Optional<String> found = getModuleNameToGA().get().entrySet().stream().filter(
                    e -> e.getValue().equals(groupArtifact)).map(Map.Entry::getKey).findFirst();
            if (found.isPresent()) {
                return found.get();
            } else {
                String[] split = groupArtifact.split(":");
                String group = split[0];
                String artifact = split[1];
                Optional<String> modulePrefix = getModuleNamePrefixToGroup().get().entrySet().stream().filter(
                        e -> e.getValue().equals(group)).map(Map.Entry::getKey).findFirst();
                return modulePrefix.map(s -> s + artifact).orElse(null);
            }
        });
    }

    /**
     * Use consistent resolution to manage versions consistently through in the main application project(s).
     *
     * @param versionsProvidingProjects projects which runtime classpaths are the runtime classpaths of the applications/services being built.
     */
    public void versionsFromConsistentResolution(String... versionsProvidingProjects) {
        Configuration mainRuntimeClasspath = getConfigurations().create("mainRuntimeClasspath", c -> {
            c.setCanBeConsumed(false);
            c.getAttributes().attribute(Usage.USAGE_ATTRIBUTE, getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
        });
        getConfigurations().configureEach(c -> {
            if (c.isCanBeResolved() && !c.isCanBeConsumed() && c != mainRuntimeClasspath) {
                c.shouldResolveConsistentlyWith(mainRuntimeClasspath);
            }
        });
        for (String versionsProvidingProject : versionsProvidingProjects) {
            getDependencies().add(mainRuntimeClasspath.getName(), createDependency(versionsProvidingProject));
        }
    }

    public void versionsFromPlatformAndConsistentResolution(String platformProject, String... versionsProvidingProjects) {
        boolean platformInJavaProject = Arrays.asList(versionsProvidingProjects).contains(platformProject);
        getSourceSets().configureEach(sourceSet -> getConfigurations().getByName(sourceSet.getImplementationConfigurationName()).withDependencies(d -> {
            Dependency platformDependency = getDependencies().platform(createDependency(platformProject));
            if (platformInJavaProject) {
                if (platformProject.startsWith(":")) {
                    String capability = ((ProjectDependency) platformDependency).getDependencyProject().getGroup() + platformProject + "-platform";
                    ((ProjectDependency) platformDependency).capabilities(c -> c.requireCapability(capability));
                } else if (platformDependency instanceof ModuleDependency) {
                    String capability = platformProject + "-platform";
                    ((ModuleDependency) platformDependency).capabilities(c -> c.requireCapability(capability));
                }
            }
            d.add(platformDependency);
        }));

        versionsFromConsistentResolution(versionsProvidingProjects);
    }

    private Dependency createDependency(String project) {
        boolean isProjectInBuild = project.startsWith(":");
        return getDependencies().create(isProjectInBuild
                ? getDependencies().project(Collections.singletonMap("path", project))
                : project);
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

    private <T> Provider<T> errorIfNotFound(String moduleName) {
        return getProviders().provider(() -> {
            throw new RuntimeException("Unknown Module: " + moduleName);
        });
    }

    private <T> Provider<T> errorIfNotFound(Provider<String> moduleName) {
        return getProviders().provider(() -> {
            throw new RuntimeException("Unknown Module: " + moduleName.get());
        });
    }

    private void warnVersionMissing(String moduleName, Map<String, Object> ga, File moduleInfoFile) {
        if (getWarnForMissingVersions().get()) {
            getProject().getLogger().warn("[WARN] [Java Module Dependencies] No version defined in catalog - " + ga.get(GAV.GROUP) + ":" + ga.get(GAV.ARTIFACT) + " - "
                    + moduleDebugInfo(moduleName.replace('.', '_'), moduleInfoFile, getProject().getRootDir()));
        }
    }

    private String moduleDebugInfo(String moduleName, File moduleInfoFile, File rootDir) {
        return moduleName
                + " (required in "
                + moduleInfoFile.getAbsolutePath().substring(rootDir.getAbsolutePath().length() + 1)
                + ")";
    }

    @Inject
    protected abstract Project getProject();

    @Inject
    protected abstract ObjectFactory getObjects();

    @Inject
    protected abstract ProviderFactory getProviders();

    @Inject
    protected abstract ProjectLayout getLayout();

    @Inject
    protected abstract DependencyHandler getDependencies();

    @Inject
    protected abstract ConfigurationContainer getConfigurations();

    @Inject
    protected abstract SourceSetContainer getSourceSets();

    ModuleInfoCache getModuleInfoCache() {
        return moduleInfoCache;
    }
}
