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

import org.gradle.api.GradleException;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.VersionCatalogsExtension;
import org.gradle.api.initialization.Settings;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.util.GradleVersion;
import org.gradlex.javamodule.dependencies.dsl.AllDirectives;
import org.gradlex.javamodule.dependencies.dsl.GradleOnlyDirectives;
import org.gradlex.javamodule.dependencies.initialization.JavaModuleDependenciesSettingsPlugin;
import org.gradlex.javamodule.dependencies.internal.bridges.DependencyAnalysisBridge;
import org.gradlex.javamodule.dependencies.internal.bridges.ExtraJavaModuleInfoBridge;
import org.gradlex.javamodule.dependencies.internal.dsl.AllDirectivesInternal;
import org.gradlex.javamodule.dependencies.internal.dsl.GradleOnlyDirectivesInternal;
import org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo;
import org.gradlex.javamodule.dependencies.tasks.BuildFileDependenciesGenerate;
import org.gradlex.javamodule.dependencies.tasks.ModuleDependencyReport;
import org.gradlex.javamodule.dependencies.tasks.ModuleDirectivesOrderingCheck;
import org.gradlex.javamodule.dependencies.tasks.ModuleInfoGenerate;
import org.gradlex.javamodule.dependencies.tasks.ModulePathAnalysis;
import org.gradlex.javamodule.dependencies.tasks.ModuleVersionRecommendation;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.gradle.api.plugins.HelpTasksPlugin.HELP_GROUP;
import static org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP;
import static org.gradlex.javamodule.dependencies.JavaModuleDependenciesExtension.JAVA_MODULE_DEPENDENCIES;
import static org.gradlex.javamodule.dependencies.internal.utils.DependencyDeclarationsUtil.declaredDependencies;
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES;
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES_RUNTIME;
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES_STATIC;
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES_STATIC_TRANSITIVE;
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES_TRANSITIVE;
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleNamingUtil.sourceSetToModuleName;

@SuppressWarnings("unused")
@NonNullApi
public abstract class JavaModuleDependenciesPlugin implements Plugin<ExtensionAware> {

    private static final String EXTRA_JAVA_MODULE_INFO_PLUGIN_ID = "org.gradlex.extra-java-module-info";
    private static final String REGISTER_HELP_TASKS_PROPERTY = "org.gradlex.java-module-dependencies.register-help-tasks";

    @Override
    public void apply(ExtensionAware projectOrSettings) {
        if (GradleVersion.current().compareTo(GradleVersion.version("7.4")) < 0) {
            throw new GradleException("This plugin requires Gradle 7.4+");
        }
        if (projectOrSettings instanceof Project) {
            applyProject((Project) projectOrSettings);
        } else if (projectOrSettings instanceof Settings) {
            ((Settings) projectOrSettings).getPlugins().apply(JavaModuleDependenciesSettingsPlugin.class);
        }
    }

    private void applyProject(Project project) {
        VersionCatalogsExtension versionCatalogs = project.getExtensions().findByType(VersionCatalogsExtension.class);
        JavaModuleDependenciesExtension javaModuleDependencies = project.getExtensions().create(
                JAVA_MODULE_DEPENDENCIES, JavaModuleDependenciesExtension.class, versionCatalogs);

        setupExtraJavaModulePluginBridge(project, javaModuleDependencies);

        project.getPlugins().withType(JavaPlugin.class, javaPlugin -> setupForJavaProject(project, javaModuleDependencies));
    }

    private void setupForJavaProject(Project project, JavaModuleDependenciesExtension javaModuleDependencies) {
        boolean registerHelpTasks = Boolean.parseBoolean(project.getProviders()
                .gradleProperty(REGISTER_HELP_TASKS_PROPERTY)
                .getOrElse("true"));

        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        sourceSets.all(sourceSet -> {
            process(REQUIRES, sourceSet.getImplementationConfigurationName(), sourceSet, project, javaModuleDependencies);
            process(REQUIRES_STATIC, sourceSet.getCompileOnlyConfigurationName(), sourceSet, project, javaModuleDependencies);
            process(REQUIRES_TRANSITIVE, sourceSet.getApiConfigurationName(), sourceSet, project, javaModuleDependencies);
            process(REQUIRES_STATIC_TRANSITIVE, sourceSet.getCompileOnlyApiConfigurationName(), sourceSet, project, javaModuleDependencies);
            process(REQUIRES_RUNTIME, sourceSet.getRuntimeOnlyConfigurationName(), sourceSet, project, javaModuleDependencies);

            javaModuleDependencies.doAddRequiresRuntimeSupport(sourceSet, sourceSet);
        });

        setupDirectivesDSL(project, javaModuleDependencies);

        TaskProvider<Task> checkAllModuleInfo = registerHelpTasks ? project.getTasks().register("checkAllModuleInfo", t -> {
            t.setGroup(VERIFICATION_GROUP);
            t.setDescription("Check scope and order of directives in 'module-info.java' files");
        }) : null;

        if (registerHelpTasks) {
            setupOrderingCheckTasks(project, checkAllModuleInfo, javaModuleDependencies);
            setupModuleDependenciesTask(project);
            setupReportTasks(project, javaModuleDependencies);
            setupMigrationTasks(project, javaModuleDependencies);
        }

        project.getPlugins().withId("com.autonomousapps.dependency-analysis", analysisPlugin ->
                DependencyAnalysisBridge.registerDependencyAnalysisPostProcessingTask(project, checkAllModuleInfo));
    }

    private void setupExtraJavaModulePluginBridge(Project project, JavaModuleDependenciesExtension javaModuleDependencies) {
        project.getPlugins().withId(EXTRA_JAVA_MODULE_INFO_PLUGIN_ID,
                e -> ExtraJavaModuleInfoBridge.autoRegisterPatchedModuleMappings(project, javaModuleDependencies));
    }

    private void setupDirectivesDSL(Project project, JavaModuleDependenciesExtension javaModuleDependencies) {
        ExtensionContainer extensions = project.getExtensions();
        SourceSetContainer sourceSets = extensions.getByType(SourceSetContainer.class);
        SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        sourceSets.all(sourceSet -> {
            File moduleInfoFile = new File(sourceSet.getJava().getSrcDirs().iterator().next(), "module-info.java");
            String extensionName = sourceSet.getName() + "ModuleInfo";
            if (moduleInfoFile.exists()) {
                extensions.create(GradleOnlyDirectives.class, extensionName, GradleOnlyDirectivesInternal.class, sourceSet, mainSourceSet, javaModuleDependencies);
            } else {
                extensions.create(AllDirectives.class, extensionName, AllDirectivesInternal.class, sourceSet, mainSourceSet, javaModuleDependencies);
            }
        });
    }

    private void setupModuleDependenciesTask(Project project) {
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        TaskProvider<ModuleDependencyReport> moduleDependencies = project.getTasks().register("moduleDependencies", ModuleDependencyReport.class, t -> t.setGroup(HELP_GROUP));
        sourceSets.all(sourceSet -> moduleDependencies.configure(t -> {
            HashSet<Configuration> joined = new HashSet<>();
            if (t.getConfigurations() != null) {
                joined.addAll(t.getConfigurations());
            }
            joined.add(project.getConfigurations().getByName(sourceSet.getCompileClasspathConfigurationName()));
            joined.add(project.getConfigurations().getByName(sourceSet.getRuntimeClasspathConfigurationName()));
            t.setConfigurations(joined);
        }));
    }

    private void setupReportTasks(Project project, JavaModuleDependenciesExtension javaModuleDependencies) {
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        project.getTasks().register("analyzeModulePath", ModulePathAnalysis.class, t -> {
            t.setGroup(HELP_GROUP);
            t.setDescription("Check consistency of the Module Path");

            for (SourceSet sourceSet : sourceSets) {
                t.getClasspathConfigurations().add(project.getConfigurations().getByName(sourceSet.getCompileClasspathConfigurationName()));
                t.getClasspathConfigurations().add(project.getConfigurations().getByName(sourceSet.getRuntimeClasspathConfigurationName()));
            }
        });
        project.getTasks().register("recommendModuleVersions", ModuleVersionRecommendation.class, t -> {
            t.setGroup(HELP_GROUP);
            t.setDescription("Query repositories for latest stable versions of the used Java Modules");

            t.getPrintForPlatform().convention(true);
            t.getPrintForCatalog().convention(false);
        });
    }

    private void setupMigrationTasks(Project project, JavaModuleDependenciesExtension javaModuleDependencies) {
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        ConfigurationContainer configurations = project.getConfigurations();

        TaskProvider<Task> generateAllModuleInfoFiles = project.getTasks().register("generateAllModuleInfoFiles", t -> {
            t.setGroup("java modules");
            t.setDescription("Generate 'module-info.java' files in all source sets");
        });

        sourceSets.all(sourceSet -> {
            TaskProvider<ModuleInfoGenerate> generateModuleInfo = project.getTasks().register(sourceSet.getTaskName("generate", "ModuleInfoFile"), ModuleInfoGenerate.class, t -> {
                t.setGroup("java modules");
                t.setDescription("Generate 'module-info.java' in '" + sourceSet.getName() + "' source set");

                t.getModuleNameToGA().putAll(javaModuleDependencies.getModuleNameToGA());

                t.getModuleName().convention(project.provider(() -> project.getGroup() + "." + sourceSetToModuleName(project.getName(), sourceSet.getName())));

                t.getApiDependencies().convention(declaredDependencies(project, sourceSet.getApiConfigurationName()));
                t.getImplementationDependencies().convention(declaredDependencies(project, sourceSet.getImplementationConfigurationName()));
                t.getCompileOnlyApiDependencies().convention(declaredDependencies(project, sourceSet.getCompileOnlyApiConfigurationName()));
                t.getCompileOnlyDependencies().convention(declaredDependencies(project, sourceSet.getCompileOnlyConfigurationName()));
                t.getRuntimeOnlyDependencies().convention(declaredDependencies(project, sourceSet.getRuntimeOnlyConfigurationName()));

                t.getModuleInfoFile().convention(project.getLayout().file(project.provider(() ->
                        new File(sourceSet.getJava().getSrcDirs().iterator().next(), "module-info.java"))));
            });

            generateAllModuleInfoFiles.configure(t -> t.dependsOn(generateModuleInfo));
        });

        project.getTasks().register("generateBuildFileDependencies", BuildFileDependenciesGenerate.class, t -> {
            t.setGroup("java modules");
            t.setDescription("Generate 'dependencies' block in 'build.gradle.kts'");

            t.getOwnProjectGroup().set(project.provider(() -> project.getGroup().toString()));
            t.getWithCatalog().set(true);
            sourceSets.all(sourceSet -> t.addDependencies(sourceSet.getName(),
                    collectDependencies(project, javaModuleDependencies, sourceSet, REQUIRES_TRANSITIVE, sourceSet.getApiConfigurationName()),
                    collectDependencies(project, javaModuleDependencies, sourceSet, REQUIRES, sourceSet.getImplementationConfigurationName()),
                    collectDependencies(project, javaModuleDependencies, sourceSet, REQUIRES_STATIC_TRANSITIVE, sourceSet.getCompileOnlyApiConfigurationName()),
                    collectDependencies(project, javaModuleDependencies, sourceSet, REQUIRES_STATIC, sourceSet.getCompileOnlyConfigurationName()),
                    collectDependencies(project, javaModuleDependencies, sourceSet, REQUIRES_RUNTIME, sourceSet.getRuntimeOnlyConfigurationName())
            ));

            t.getBuildFile().set(project.getLayout().getProjectDirectory().file("build.gradle.kts"));
        });
    }

    private void setupOrderingCheckTasks(Project project, TaskProvider<Task> checkAllModuleInfo, JavaModuleDependenciesExtension javaModuleDependencies) {
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        ConfigurationContainer configurations = project.getConfigurations();

        sourceSets.configureEach(sourceSet -> {
            TaskProvider<ModuleDirectivesOrderingCheck> checkModuleInfo = project.getTasks().register(sourceSet.getTaskName("check", "ModuleInfo"), ModuleDirectivesOrderingCheck.class, t -> {
                t.setGroup("java modules");
                t.setDescription("Check order of directives in 'module-info.java' in '" + sourceSet.getName() + "' source set");

                ModuleInfo moduleInfo = javaModuleDependencies.getModuleInfoCache().get().get(sourceSet, project.getProviders());
                File folder = javaModuleDependencies.getModuleInfoCache().get().getFolder(sourceSet, project.getProviders());
                if (folder != null) {
                    t.getModuleInfoPath().convention(new File(folder, "module-info.java").getAbsolutePath());
                }
                t.getModuleNamePrefix().convention(moduleInfo.moduleNamePrefix(project.getName(), sourceSet.getName(), false));
                t.getModuleInfo().convention(moduleInfo);

                t.getReport().convention(project.getLayout().getBuildDirectory().file("reports/module-info-analysis/" + sourceSet.getName() + "-ordering.txt"));
            });

            checkAllModuleInfo.configure(t -> t.dependsOn(checkModuleInfo));
        });
    }

    private void process(ModuleInfo.Directive moduleDirective, String gradleConfiguration, SourceSet sourceSet, Project project, JavaModuleDependenciesExtension javaModuleDependenciesExtension) {
        Configuration conf = project.getConfigurations().findByName(gradleConfiguration);
        if (conf != null) {
            conf.withDependencies(d -> readModuleInfo(moduleDirective, sourceSet, project, conf, javaModuleDependenciesExtension));
        } else {
            project.getConfigurations().whenObjectAdded(lateAddedConf -> {
                if (gradleConfiguration.equals(lateAddedConf.getName())) {
                    lateAddedConf.withDependencies(d -> readModuleInfo(moduleDirective, sourceSet, project, lateAddedConf, javaModuleDependenciesExtension));
                }
            });
        }
    }

    private void readModuleInfo(ModuleInfo.Directive moduleDirective, SourceSet sourceSet, Project project, Configuration configuration, JavaModuleDependenciesExtension javaModuleDependenciesExtension) {
        if (javaModuleDependenciesExtension.getAnalyseOnly().get()) {
            return;
        }
        ModuleInfo moduleInfo = javaModuleDependenciesExtension.getModuleInfoCache().get().get(sourceSet, project.getProviders());
        for (String moduleName : moduleInfo.get(moduleDirective)) {
            declareDependency(moduleName,  project, sourceSet, configuration, javaModuleDependenciesExtension);
        }
    }

    private void declareDependency(String moduleName, Project project, SourceSet sourceSet, Configuration configuration, JavaModuleDependenciesExtension javaModuleDependencies) {
        if (JDKInfo.MODULES.contains(moduleName)) {
            // The module is part of the JDK, no dependency required
            return;
        }

        project.getDependencies().addProvider(configuration.getName(), javaModuleDependencies.create(moduleName, sourceSet));
    }

    private List<BuildFileDependenciesGenerate.DependencyDeclaration> collectDependencies(Project project, JavaModuleDependenciesExtension javaModuleDependencies, SourceSet sourceSet, ModuleInfo.Directive directive, String scope) {
        ModuleInfo moduleInfo = javaModuleDependencies.getModuleInfoCache().get().get(sourceSet, project.getProviders());
        if (moduleInfo == ModuleInfo.EMPTY) {
            // check if there is a whiltebox module-info we can use isntead
            File sourceSetDir = sourceSet.getJava().getSrcDirs().iterator().next().getParentFile();
            File whiteboxModuleInfoFile = new File(sourceSetDir, "java9/module-info.java");
            if (whiteboxModuleInfoFile.exists()) {
                moduleInfo = new ModuleInfo(project.getProviders().fileContents(project.getLayout().getProjectDirectory().file(whiteboxModuleInfoFile.getAbsolutePath())).getAsText().get());
            }
        }
        return moduleInfo.get(directive).stream()
                .map(moduleName -> new BuildFileDependenciesGenerate.DependencyDeclaration(scope, moduleName, javaModuleDependencies.ga(moduleName)))
                .collect(Collectors.toList());
    }

}
