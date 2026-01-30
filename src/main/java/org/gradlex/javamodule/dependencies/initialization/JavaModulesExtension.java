// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.initialization;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.IsolatedAction;
import org.gradle.api.Project;
import org.gradle.api.initialization.ProjectDescriptor;
import org.gradle.api.initialization.Settings;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.JavaApplication;
import org.gradle.api.plugins.JavaPlatformExtension;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradlex.javamodule.dependencies.JavaModuleDependenciesExtension;
import org.gradlex.javamodule.dependencies.JavaModuleDependenciesPlugin;
import org.gradlex.javamodule.dependencies.JavaModuleVersionsPlugin;
import org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo;
import org.gradlex.javamodule.dependencies.internal.utils.ModuleInfoCache;
import org.gradlex.javamodule.dependencies.internal.utils.ValueModuleDirectoryListing;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class JavaModulesExtension {

    private final Settings settings;
    private final ModuleInfoCache moduleInfoCache;
    private final List<ModuleProject> moduleProjects = new ArrayList<>();

    @Inject
    public abstract ObjectFactory getObjects();

    @Inject
    public abstract ProviderFactory getProviders();

    @Inject
    public JavaModulesExtension(Settings settings) {
        this.settings = settings;
        this.moduleInfoCache = getObjects().newInstance(ModuleInfoCache.class, true);
        settings.getGradle().getLifecycle().beforeProject(new ApplyPluginsAction(moduleProjects, moduleInfoCache));
    }

    /**
     * {@link JavaModulesExtension#module(String, Action)}
     */
    public void module(String directory) {
        module(directory, m -> {});
    }

    /**
     * Register and configure Module located in the given folder, relative to the build root directory.
     */
    public void module(String directory, Action<Module> action) {
        Module module = getObjects().newInstance(Module.class, new File(settings.getRootDir(), directory));
        action.execute(module);
        includeModule(module, new File(settings.getRootDir(), directory), false);
    }

    /**
     * {@link JavaModulesExtension#module(ProjectDescriptor, Action)}
     */
    public void module(ProjectDescriptor project) {
        module(project, m -> {});
    }

    /**
     * Register and configure Module already registered as project by an 'include' statement.
     */
    public void module(ProjectDescriptor project, Action<Module> action) {
        Module module = getObjects().newInstance(Module.class, project.getProjectDir());
        module.getArtifact().set(project.getName());
        module.getArtifact().finalizeValue(); // finalize, as the project name can no longer be changed
        action.execute(module);
        configureModule(module, project, false);
    }

    /**
     * {@link JavaModulesExtension#directory(String, Action)}
     */
    public void directory(String directory) {
        directory(directory, m -> {});
    }

    /**
     * Register and configure ALL Modules located in direct subfolders of the given folder.
     */
    public void directory(String directory, Action<Directory> action) {
        File modulesDirectory = new File(settings.getRootDir(), directory);
        Directory moduleDirectory = getObjects().newInstance(Directory.class, modulesDirectory);
        action.execute(moduleDirectory);

        for (Module module : moduleDirectory.customizedModules.values()) {
            includeModule(module, module.directory, false);
        }
        Provider<List<String>> listProvider = getProviders().of(ValueModuleDirectoryListing.class, spec -> {
            spec.getParameters().getExclusions().set(moduleDirectory.getExclusions());
            spec.getParameters().getExplicitlyConfiguredFolders().set(moduleDirectory.customizedModules.keySet());
            spec.getParameters().getDir().set(modulesDirectory);
            spec.getParameters().getRequiresBuildFile().set(moduleDirectory.getRequiresBuildFile());
        });

        for (String projectDir : listProvider.get()) {
            Module module = moduleDirectory.addModule(projectDir);
            if (!module.getModuleInfoPaths().get().isEmpty()) {
                // only auto-include if there is at least one module-info.java
                includeModule(module, new File(modulesDirectory, projectDir), false);
            }
        }
    }

    /**
     * Configure a subproject as Platform for defining Module versions.
     */
    public void versions(String directory) {
        versions(directory, m -> {});
    }

    /**
     * Configure a subproject as Platform for defining Module versions.
     */
    public void versions(String directory, Action<Module> action) {
        File versionProjectDir = new File(settings.getRootDir(), directory);
        Module module = getObjects().newInstance(Module.class, versionProjectDir);
        action.execute(module);
        includeModule(module, module.directory, true);
    }

    private void includeModule(Module module, File projectDir, boolean definesVersions) {
        String artifact = module.getArtifact().get();
        settings.include(artifact);
        ProjectDescriptor project = settings.project(":" + artifact);
        project.setProjectDir(projectDir);

        configureModule(module, project, definesVersions);
    }

    private void configureModule(Module module, ProjectDescriptor project, boolean definesVersions) {
        String mainModuleName = null;
        if (!definesVersions) {
            for (String moduleInfoPath : module.getModuleInfoPaths().get()) {
                ModuleInfo moduleInfo = moduleInfoCache.put(
                        project.getProjectDir(),
                        moduleInfoPath,
                        project.getPath(),
                        module.getArtifact().get(),
                        module.getGroup(),
                        settings.getProviders());
                if (moduleInfoPath.contains("/main/")) {
                    mainModuleName = moduleInfo.getModuleName();
                }
            }
        }

        String group = module.getGroup().getOrNull();
        List<String> plugins = module.getPlugins().get();
        moduleProjects.add(new ModuleProject(project.getPath(), group, plugins, mainModuleName, definesVersions));
    }

    private static class ModuleProject {
        private final String path;
        private final @Nullable String group;
        private final List<String> plugins;
        private final @Nullable String mainModuleName;
        private final boolean definesVersions;

        public ModuleProject(
                String path,
                @Nullable String group,
                List<String> plugins,
                @Nullable String mainModuleName,
                boolean definesVersions) {
            this.path = path;
            this.group = group;
            this.plugins = plugins;
            this.mainModuleName = mainModuleName;
            this.definesVersions = definesVersions;
        }
    }

    private static class ApplyPluginsAction implements IsolatedAction<Project> {

        private final List<ModuleProject> moduleProjects;
        private final ModuleInfoCache moduleInfoCache;

        public ApplyPluginsAction(List<ModuleProject> moduleProjects, ModuleInfoCache moduleInfoCache) {
            this.moduleProjects = moduleProjects;
            this.moduleInfoCache = moduleInfoCache;
        }

        @Override
        public void execute(Project project) {
            for (ModuleProject m : moduleProjects) {
                if (project.getPath().equals(m.path)) {
                    if (m.group != null) project.setGroup(m.group);
                    if (m.definesVersions) {
                        project.getPlugins().apply(JavaPlatformPlugin.class);
                        project.getPlugins().apply(JavaModuleVersionsPlugin.class);
                        project.getExtensions()
                                .getByType(JavaPlatformExtension.class)
                                .allowDependencies();
                    } else {
                        project.getPlugins().apply(JavaModuleDependenciesPlugin.class);
                        project.getExtensions()
                                .getByType(JavaModuleDependenciesExtension.class)
                                .getModuleInfoCache()
                                .set(moduleInfoCache);
                    }
                    m.plugins.forEach(id -> project.getPlugins().apply(id));
                    if (m.mainModuleName != null) {
                        project.getPlugins().withType(ApplicationPlugin.class, p -> project.getExtensions()
                                .getByType(JavaApplication.class)
                                .getMainModule()
                                .set(m.mainModuleName));
                    }
                }
            }
        }
    }
}
