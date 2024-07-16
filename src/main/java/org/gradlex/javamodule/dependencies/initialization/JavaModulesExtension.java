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

package org.gradlex.javamodule.dependencies.initialization;

import org.gradle.api.Action;
import org.gradle.api.IsolatedAction;
import org.gradle.api.NonNullApi;
import org.gradle.api.Project;
import org.gradle.api.initialization.ProjectDescriptor;
import org.gradle.api.initialization.Settings;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.JavaApplication;
import org.gradle.api.plugins.JavaPlatformExtension;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradlex.javamodule.dependencies.JavaModuleDependenciesExtension;
import org.gradlex.javamodule.dependencies.JavaModuleDependenciesPlugin;
import org.gradlex.javamodule.dependencies.JavaModuleVersionsPlugin;
import org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo;
import org.gradlex.javamodule.dependencies.internal.utils.ModuleInfoCache;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.File;
import java.nio.file.Paths;
import java.util.List;

public abstract class JavaModulesExtension {

    private final Settings settings;
    private final ModuleInfoCache moduleInfoCache;

    @Inject
    public abstract ObjectFactory getObjects();

    @Inject
    public JavaModulesExtension(Settings settings) {
        this.settings = settings;
        this.moduleInfoCache = getObjects().newInstance(ModuleInfoCache.class, true);
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
        Module module = getObjects().newInstance(Module.class, settings.getRootDir());
        module.getDirectory().set(directory);
        action.execute(module);
        includeModule(module, new File(settings.getRootDir(), module.getDirectory().get()));
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

        File[] projectDirs = modulesDirectory.listFiles();
        if (projectDirs == null) {
            throw new RuntimeException("Failed to inspect: " + modulesDirectory);
        }

        for (Module module : moduleDirectory.customizedModules.values()) {
            includeModule(module, new File(modulesDirectory, module.getDirectory().get()));
        }

        for (File projectDir : projectDirs) {
            if (!moduleDirectory.customizedModules.containsKey(projectDir.getName())) {
                Module module = moduleDirectory.addModule(projectDir.getName());
                if (!module.getModuleInfoPaths().get().isEmpty()) {
                    // only auto-include if there is at least one module-info.java
                    includeModule(module, projectDir);
                }
            }
        }
    }

    /**
     * Configure a subproject as Platform for defining Module versions.
     */
    public void versions(String directory) {
        String projectName = Paths.get(directory).getFileName().toString();
        settings.include(projectName);
        settings.project(":" + projectName).setProjectDir(new File(settings.getRootDir(), directory));
        settings.getGradle().getLifecycle().beforeProject(new ApplyJavaModuleVersionsPluginAction(projectName));
    }

    private void includeModule(Module module, File projectDir) {
        String artifact = module.getArtifact().get();
        settings.include(artifact);
        ProjectDescriptor project = settings.project(":" + artifact);
        project.setProjectDir(projectDir);

        String mainModuleName = null;
        for (String path : module.getModuleInfoPaths().get()) {
            ModuleInfo moduleInfo = moduleInfoCache.put(projectDir, path,
                    module.getArtifact().get(), module.getGroup(), settings.getProviders());
            if (path.contains("/main/")) {
                mainModuleName = moduleInfo.getModuleName();
            }
        }

        String group = module.getGroup().getOrNull();
        List<String> plugins = module.getPlugins().get();
        settings.getGradle().getLifecycle().beforeProject(new ApplyPluginsAction(artifact, group, plugins, mainModuleName, moduleInfoCache));
    }

    @NonNullApi
    private static class ApplyPluginsAction implements IsolatedAction<Project>, Action<Project> {

        private final String artifact;
        private final String group;
        private final List<String> plugins;
        private final String mainModuleName;
        private final ModuleInfoCache moduleInfoCache;

        public ApplyPluginsAction(String artifact, @Nullable String group, List<String> plugins, @Nullable String mainModuleName, ModuleInfoCache moduleInfoCache) {
            this.artifact = artifact;
            this.group = group;
            this.plugins = plugins;
            this.mainModuleName = mainModuleName;
            this.moduleInfoCache = moduleInfoCache;
        }

        @Override
        public void execute(Project project) {
            if (project.getName().equals(artifact)) {
                if (group != null) project.setGroup(group);
                project.getPlugins().apply(JavaModuleDependenciesPlugin.class);
                project.getExtensions().getByType(JavaModuleDependenciesExtension.class).getModuleInfoCache().set(moduleInfoCache);
                plugins.forEach(id -> project.getPlugins().apply(id));
                if (mainModuleName != null) {
                    project.getPlugins().withType(ApplicationPlugin.class, p ->
                            project.getExtensions().getByType(JavaApplication.class).getMainModule().set(mainModuleName));
                }
            }
        }
    }

    @NonNullApi
    private static class ApplyJavaModuleVersionsPluginAction implements IsolatedAction<Project>, Action<Project> {

        private final String projectName;

        public ApplyJavaModuleVersionsPluginAction(String projectName) {
            this.projectName = projectName;
        }

        @Override
        public void execute(Project project) {
            if (projectName.equals(project.getName())) {
                project.getPlugins().apply(JavaPlatformPlugin.class);
                project.getPlugins().apply(JavaModuleVersionsPlugin.class);
                project.getExtensions().getByType(JavaPlatformExtension.class).allowDependencies();
            }
        }
    }
}
