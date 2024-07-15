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
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.util.GradleVersion;
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

    static final boolean SUPPORT_PROJECT_ISOLATION =
            GradleVersion.current().compareTo(GradleVersion.version("8.8")) >= 0;

    private final Settings settings;
    private final ModuleInfoCache moduleInfoCache;

    @Inject
    public abstract ObjectFactory getObjects();

    @Inject
    public JavaModulesExtension(Settings settings) {
        this.settings = settings;
        this.moduleInfoCache = getObjects().newInstance(ModuleInfoCache.class);
    }

    public void module(String folder) {
        module(folder, m -> {});
    }

    public void module(String folder, Action<Module> action) {
        Module module = getObjects().newInstance(Module.class, settings.getRootDir());
        module.getFolder().set(folder);
        action.execute(module);
        includeModule(module, new File(settings.getRootDir(), module.getFolder().get()));
    }

    public void modules(String folder) {
        modules(folder, m -> {});
    }

    public void modules(String folder, Action<Modules> action) {
        Modules moduleGroup = getObjects().newInstance(Modules.class, new File(settings.getRootDir(), folder));
        action.execute(moduleGroup);

        File[] projectFolders = new File(settings.getRootDir(), folder).listFiles();
        if (projectFolders == null) {
            throw new RuntimeException("Failed to inspect: " + new File(settings.getRootDir(), folder));
        }

        for (File projectFolder : projectFolders) {
            if (moduleGroup.customizedModules.containsKey(projectFolder.getName())) {
                includeModule(moduleGroup.customizedModules.get(projectFolder.getName()), projectFolder);
            } else {
                includeModule(moduleGroup.addModule(projectFolder.getName()), projectFolder);
            }
        }
    }

    public void versions(String folder) {
        String projectName = Paths.get(folder).getFileName().toString();
        settings.include(projectName);
        settings.project(":" + projectName).setProjectDir(new File(settings.getRootDir(), folder));
        if (SUPPORT_PROJECT_ISOLATION) {
            settings.getGradle().getLifecycle().beforeProject(new ApplyJavaModuleVersionsPluginAction(projectName));
        } else {
            settings.getGradle().beforeProject(new ApplyJavaModuleVersionsPluginAction(projectName));
        }
    }

    private void includeModule(Module module, File projectDir) {
        List<String> modulePaths = module.getModuleInfoPaths().get();
        if (modulePaths.isEmpty()) {
            return;
        }

        String artifact = module.getArtifact().get();
        settings.include(artifact);
        ProjectDescriptor project = settings.project(":" + artifact);
        project.setProjectDir(projectDir);

        String mainModuleName = null;
        for (String path : modulePaths) {
            ModuleInfo moduleInfo = moduleInfoCache.put(projectDir, path,
                    module.getArtifact().get(), module.getGroup(), settings.getProviders());
            if (path.contains("/main/")) {
                mainModuleName = moduleInfo.getModuleName();
            }
        }

        String group = module.getGroup().getOrNull();
        List<String> plugins = module.getPlugins().get();
        if (SUPPORT_PROJECT_ISOLATION) {
            settings.getGradle().getLifecycle().beforeProject(new ApplyPluginsAction(artifact, group, plugins, mainModuleName));
        } else {
            settings.getGradle().beforeProject(new ApplyPluginsAction(artifact, group, plugins, mainModuleName));
        }
    }

    @NonNullApi
    private static class ApplyPluginsAction implements IsolatedAction<Project>, Action<Project> {

        private final String artifact;
        private final String group;
        private final List<String> plugins;
        private final String mainModuleName;

        public ApplyPluginsAction(String artifact, @Nullable String group, List<String> plugins, @Nullable String mainModuleName) {
            this.artifact = artifact;
            this.group = group;
            this.plugins = plugins;
            this.mainModuleName = mainModuleName;
        }

        @Override
        public void execute(Project project) {
            if (project.getName().equals(artifact)) {
                if (group != null) project.setGroup(group);
                project.getPlugins().apply(JavaModuleDependenciesPlugin.class);
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
            }
        }
    }
}
