package de.jjohannes.gradle.moduledependencies;

import de.jjohannes.gradle.moduledependencies.tasks.RecommendModuleVersionsReportTask;
import de.jjohannes.gradle.moduledependencies.tasks.ShowModuleNameMappingsReportTask;
import org.gradle.api.GradleException;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.VersionCatalogsExtension;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.util.GradleVersion;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static de.jjohannes.gradle.moduledependencies.JavaModuleDependenciesExtension.JAVA_MODULE_DEPENDENCIES;

@SuppressWarnings({"unused", "UnstableApiUsage"})
@NonNullApi
public abstract class JavaModuleDependenciesPlugin implements Plugin<Project> {

    private final Map<File, ModuleInfo> moduleInfo = new HashMap<>();

    @Override
    public void apply(Project project) {
        if (GradleVersion.current().compareTo(GradleVersion.version("7.2")) < 0) {
            throw new GradleException("This plugin requires Gradle 7.2+");
        }

        project.getPlugins().apply(JavaPlugin.class);

        VersionCatalogsExtension versionCatalogs = project.getExtensions().findByType(VersionCatalogsExtension.class);

        JavaModuleDependenciesExtension javaModuleDependenciesExtension = project.getExtensions().create(
                JAVA_MODULE_DEPENDENCIES, JavaModuleDependenciesExtension.class, versionCatalogs, project.getLogger());
        javaModuleDependenciesExtension.getOwnModuleNamesPrefix().convention(
                project.provider(() -> project.getGroup().toString()));
        javaModuleDependenciesExtension.getWarnForMissingVersions().convention(true);
        javaModuleDependenciesExtension.getVersionCatalogName().convention("libs");

        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        for (SourceSet sourceSet : sourceSets) {
            process(ModuleInfo.Directive.REQUIRES, sourceSet.getImplementationConfigurationName(), sourceSet, project, javaModuleDependenciesExtension);
            process(ModuleInfo.Directive.REQUIRES_STATIC, sourceSet.getCompileOnlyConfigurationName(), sourceSet, project, javaModuleDependenciesExtension);
        }
        project.getPlugins().withType(JavaLibraryPlugin.class, p -> {
            for (SourceSet sourceSet : sourceSets) {
                process(ModuleInfo.Directive.REQUIRES_TRANSITIVE, sourceSet.getApiConfigurationName(), sourceSet, project, javaModuleDependenciesExtension);
                process(ModuleInfo.Directive.REQUIRES_STATIC_TRANSITIVE, sourceSet.getCompileOnlyApiConfigurationName(), sourceSet, project, javaModuleDependenciesExtension);
            }
        });

        setupReportTasks(project, javaModuleDependenciesExtension);
    }

    private void setupReportTasks(Project project, JavaModuleDependenciesExtension javaModuleDependencies) {
        project.getTasks().register("showModuleNameMappings", ShowModuleNameMappingsReportTask.class);
        project.getTasks().register("recommendModuleVersions", RecommendModuleVersionsReportTask.class);
    }

    private void process(ModuleInfo.Directive moduleDirective, String gradleConfiguration, SourceSet sourceSet, Project project, JavaModuleDependenciesExtension javaModuleDependenciesExtension) {
        Configuration conf = project.getConfigurations().findByName(gradleConfiguration);
        if (conf != null) {
            conf.withDependencies(d -> findAndReadModuleInfo(moduleDirective, sourceSet, project, conf, javaModuleDependenciesExtension));
        }
    }

    private void findAndReadModuleInfo(ModuleInfo.Directive moduleDirective, SourceSet sourceSet, Project project, Configuration configuration, JavaModuleDependenciesExtension javaModuleDependenciesExtension) {
        for (File folder : sourceSet.getJava().getSrcDirs()) {
            Provider<RegularFile> moduleInfoFile = project.getLayout().file(project.provider(() -> new File(folder, "module-info.java")));
            Provider<String> moduleInfoContent = project.getProviders().fileContents(moduleInfoFile).getAsText().forUseAtConfigurationTime();
            if (moduleInfoContent.isPresent()) {
                if (!this.moduleInfo.containsKey(folder)) {
                    this.moduleInfo.put(folder, new ModuleInfo(moduleInfoContent.get()));
                }
                for (String moduleName : this.moduleInfo.get(folder).get(moduleDirective)) {
                    declareDependency(moduleName, moduleInfoFile, project, configuration, javaModuleDependenciesExtension);
                }
            }
        }
    }

    private void declareDependency(String moduleName, Provider<RegularFile> moduleInfoFile, Project project, Configuration configuration, JavaModuleDependenciesExtension javaModuleDependenciesExtension) {
        if (JDKInfo.MODULES.contains(moduleName)) {
            // The module is part of the JDK, no dependency required
            return;
        }

        String ownModuleNamesPrefix = javaModuleDependenciesExtension.getOwnModuleNamesPrefix().forUseAtConfigurationTime().get();
        String ga = javaModuleDependenciesExtension.ga(moduleName);
        String projectName =  moduleName.startsWith(ownModuleNamesPrefix + ".") ? moduleName.substring(ownModuleNamesPrefix.length() + 1) : null;

        if (projectName != null) {
            project.getDependencies().add(
                    configuration.getName(),
                    project.project(":" + projectName)
            );
        } else if (ga != null) {
            project.getDependencies().add(
                    configuration.getName(), javaModuleDependenciesExtension.gav(moduleName));
        } else {
            throw new RuntimeException("No mapping registered for module: " + moduleDebugInfo(moduleName, moduleInfoFile, project.getRootDir()) +
                    " - use 'javaModuleDependencies.moduleNameToGA.put()' to add mapping.");
        }
    }

    private String moduleDebugInfo(String moduleName, Provider<RegularFile> moduleInfo, File rootDir) {
        return moduleName
                + " (required in "
                + moduleInfo.forUseAtConfigurationTime().get().getAsFile().getAbsolutePath().substring(rootDir.getAbsolutePath().length() + 1)
                + ")";
    }

}
