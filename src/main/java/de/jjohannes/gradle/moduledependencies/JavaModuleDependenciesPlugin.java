package de.jjohannes.gradle.moduledependencies;

import de.jjohannes.gradle.moduledependencies.internal.bridges.ExtraJavaModuleInfoBridge;
import de.jjohannes.gradle.moduledependencies.internal.utils.ModuleInfo;
import de.jjohannes.gradle.moduledependencies.tasks.ModuleInfoGeneration;
import de.jjohannes.gradle.moduledependencies.tasks.ModuleVersionRecommendation;
import de.jjohannes.gradle.moduledependencies.tasks.ModulePathAnalysis;
import org.gradle.api.GradleException;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.VersionCatalogsExtension;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.util.GradleVersion;

import javax.annotation.Nullable;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.jjohannes.gradle.moduledependencies.JavaModuleDependenciesExtension.JAVA_MODULE_DEPENDENCIES;
import static de.jjohannes.gradle.moduledependencies.internal.utils.DependencyDeclarationsUtil.declaredDependencies;
import static de.jjohannes.gradle.moduledependencies.internal.utils.ModuleNamingUtil.sourceSetToModuleName;
import static org.gradle.api.plugins.HelpTasksPlugin.HELP_GROUP;

@SuppressWarnings("unused")
@NonNullApi
public abstract class JavaModuleDependenciesPlugin implements Plugin<Project> {

    private static final String EXTRA_JAVA_MODULE_INFO_PLUGIN_ID = "de.jjohannes.extra-java-module-info";

    private final Map<File, ModuleInfo> moduleInfo = new HashMap<>();

    @Override
    public void apply(Project project) {
        if (GradleVersion.current().compareTo(GradleVersion.version("7.4")) < 0) {
            throw new GradleException("This plugin requires Gradle 7.4+");
        }

        VersionCatalogsExtension versionCatalogs = project.getExtensions().findByType(VersionCatalogsExtension.class);
        JavaModuleDependenciesExtension javaModuleDependencies = project.getExtensions().create(
                JAVA_MODULE_DEPENDENCIES, JavaModuleDependenciesExtension.class, versionCatalogs);

        project.getPlugins().withType(JavaPlugin.class, javaPlugin -> setupForJavaProject(project, javaModuleDependencies));
    }

    private void setupForJavaProject(Project project, JavaModuleDependenciesExtension javaModuleDependencies) {
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        sourceSets.all(sourceSet ->  {
            process(ModuleInfo.Directive.REQUIRES, sourceSet.getImplementationConfigurationName(), sourceSet, project, javaModuleDependencies);
            process(ModuleInfo.Directive.REQUIRES_STATIC, sourceSet.getCompileOnlyConfigurationName(), sourceSet, project, javaModuleDependencies);
            process(ModuleInfo.Directive.REQUIRES_TRANSITIVE, sourceSet.getApiConfigurationName(), sourceSet, project, javaModuleDependencies);
            process(ModuleInfo.Directive.REQUIRES_STATIC_TRANSITIVE, sourceSet.getCompileOnlyApiConfigurationName(), sourceSet, project, javaModuleDependencies);
        });
        setupExtraJavaModulePluginBridge(project, javaModuleDependencies);
        setupReportTasks(project, javaModuleDependencies);
        setupMigrationTasks(project, javaModuleDependencies);
    }

    private void setupExtraJavaModulePluginBridge(Project project, JavaModuleDependenciesExtension javaModuleDependencies) {
        project.getPlugins().withId(EXTRA_JAVA_MODULE_INFO_PLUGIN_ID,
                e -> ExtraJavaModuleInfoBridge.autoRegisterPatchedModuleMappings(project, javaModuleDependencies));
    }

    private void setupReportTasks(Project project, JavaModuleDependenciesExtension javaModuleDependencies) {
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        boolean usesVersionCatalog = project.getExtensions().findByType(VersionCatalogsExtension.class) != null;
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

            t.getPrintForPlatform().convention(!usesVersionCatalog);
            t.getPrintForCatalog().convention(usesVersionCatalog);
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
            TaskProvider<ModuleInfoGeneration> generateModuleInfo = project.getTasks().register(sourceSet.getTaskName("generate", "ModuleInfoFile"), ModuleInfoGeneration.class, t -> {
                t.setGroup("java modules");
                t.setDescription("Generate 'module-info.java' in '" + sourceSet.getName() + "' source set");

                t.getModuleNameToGA().putAll(javaModuleDependencies.getModuleNameToGA());

                t.getModuleName().convention(project.provider(() -> project.getGroup() + "." + sourceSetToModuleName(project.getName(), sourceSet.getName())));

                t.getApiDependencies().convention(declaredDependencies(project, sourceSet.getApiConfigurationName()));
                t.getImplementationDependencies().convention(declaredDependencies(project, sourceSet.getImplementationConfigurationName()));
                t.getCompileOnlyApiDependencies().convention(declaredDependencies(project, sourceSet.getCompileOnlyApiConfigurationName()));
                t.getCompileOnlyDependencies().convention(declaredDependencies(project, sourceSet.getCompileOnlyConfigurationName()));

                t.getModuleInfoFile().convention(project.getLayout().file(project.provider(() ->
                        new File(sourceSet.getJava().getSrcDirs().iterator().next(), "module-info.java"))));
            });

            generateAllModuleInfoFiles.configure(t -> t.dependsOn(generateModuleInfo));
        });
    }

    private void process(ModuleInfo.Directive moduleDirective, String gradleConfiguration, SourceSet sourceSet, Project project, JavaModuleDependenciesExtension javaModuleDependenciesExtension) {
        Configuration conf = project.getConfigurations().findByName(gradleConfiguration);
        if (conf != null) {
            conf.withDependencies(d -> findAndReadModuleInfo(moduleDirective, sourceSet, project, conf, javaModuleDependenciesExtension));
        } else {
            project.getConfigurations().whenObjectAdded(lateAddedConf -> {
                if (gradleConfiguration.equals(lateAddedConf.getName())) {
                    lateAddedConf.withDependencies(d -> findAndReadModuleInfo(moduleDirective, sourceSet, project, lateAddedConf, javaModuleDependenciesExtension));
                }
            });
        }
    }

    private void findAndReadModuleInfo(ModuleInfo.Directive moduleDirective, SourceSet sourceSet, Project project, Configuration configuration, JavaModuleDependenciesExtension javaModuleDependenciesExtension) {
        for (File folder : sourceSet.getJava().getSrcDirs()) {
            Provider<RegularFile> moduleInfoFile = project.getLayout().file(project.provider(() -> new File(folder, "module-info.java")));
            Provider<String> moduleInfoContent = project.getProviders().fileContents(moduleInfoFile).getAsText();
            if (moduleInfoContent.isPresent()) {
                if (!this.moduleInfo.containsKey(folder)) {
                    this.moduleInfo.put(folder, new ModuleInfo(moduleInfoContent.get()));
                }
                ModuleInfo moduleInfo = this.moduleInfo.get(folder);
                String ownModuleNamesPrefix = moduleInfo.moduleNamePrefix(project.getName(), sourceSet.getName());
                for (String moduleName : moduleInfo.get(moduleDirective)) {
                    declareDependency(moduleName, ownModuleNamesPrefix, moduleInfoFile, project, configuration, javaModuleDependenciesExtension);
                }
            }
        }
    }

    private void declareDependency(String moduleName, @Nullable String ownModuleNamesPrefix, Provider<RegularFile> moduleInfoFile, Project project, Configuration configuration, JavaModuleDependenciesExtension javaModuleDependencies) {
        if (JDKInfo.MODULES.contains(moduleName)) {
            // The module is part of the JDK, no dependency required
            return;
        }

        Map<String, String> allProjectNamesAndGroups = project.getRootProject().getSubprojects().stream().collect(
                Collectors.toMap(Project::getName, e -> (String) project.getGroup()));

        Provider<Map<String, Object>> gav = javaModuleDependencies.gav(moduleName);
        String moduleNameSuffix = ownModuleNamesPrefix == null ? null : moduleName.startsWith(ownModuleNamesPrefix + ".") ? moduleName.substring(ownModuleNamesPrefix.length() + 1) : null;

        Optional<String> existingProjectName = allProjectNamesAndGroups.keySet().stream().filter(p -> moduleNameSuffix != null && moduleNameSuffix.startsWith(p + ".")).findFirst();

        if (allProjectNamesAndGroups.containsKey(moduleNameSuffix)) {
            project.getDependencies().add(configuration.getName(), project.project(":" + moduleNameSuffix));
        } else if (existingProjectName.isPresent()) {
            // no exact match -> add capability to point at Module in other source set
            ProjectDependency projectDependency = (ProjectDependency)
                    project.getDependencies().add(configuration.getName(), project.project(":" + existingProjectName.get()));
            assert projectDependency != null;
            projectDependency.capabilities(c -> c.requireCapabilities(
                    allProjectNamesAndGroups.get(existingProjectName.get()) + ":" + moduleNameSuffix.replace(".", "-")));
        } else if (gav.isPresent()) {
            project.getDependencies().addProvider(configuration.getName(), gav);
            if (!gav.get().containsKey(GAV.VERSION)) {
                warnVersionMissing(moduleName, gav.get(), moduleInfoFile, project, javaModuleDependencies);
            }
        } else {
            project.getLogger().lifecycle(
                    "[WARN] [Java Module Dependencies] No mapping registered for module: " + moduleDebugInfo(moduleName, moduleInfoFile, project.getRootDir()) +
                    " - use 'javaModuleDependencies.moduleNameToGA.put(\"" + moduleName + "\", \"group:artifact\")' to add mapping.");
        }
    }

    private void warnVersionMissing(String moduleName, Map<String, Object> ga, Provider<RegularFile> moduleInfoFile, Project project, JavaModuleDependenciesExtension javaModuleDependencies) {
        if (javaModuleDependencies.getWarnForMissingVersions().get()) {
            project.getLogger().warn("[WARN] [Java Module Dependencies] No version defined in catalog - " + ga.get(GAV.GROUP) + ":" + ga.get(GAV.ARTIFACT) + " - "
                    + moduleDebugInfo(moduleName.replace('.', '_'), moduleInfoFile, project.getRootDir()));
        }
    }

    private String moduleDebugInfo(String moduleName, Provider<RegularFile> moduleInfoFile, File rootDir) {
        return moduleName
                + " (required in "
                + moduleInfoFile.get().getAsFile().getAbsolutePath().substring(rootDir.getAbsolutePath().length() + 1)
                + ")";
    }

}
