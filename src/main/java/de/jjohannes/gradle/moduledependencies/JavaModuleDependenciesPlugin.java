package de.jjohannes.gradle.moduledependencies;

import de.jjohannes.gradle.moduledependencies.tasks.RecommendModuleVersionsReportTask;
import de.jjohannes.gradle.moduledependencies.tasks.AnalyzeModulePathReportTask;
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

import javax.annotation.Nullable;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static de.jjohannes.gradle.moduledependencies.JavaModuleDependenciesExtension.JAVA_MODULE_DEPENDENCIES;
import static org.gradle.api.plugins.HelpTasksPlugin.HELP_GROUP;

@SuppressWarnings({"unused", "UnstableApiUsage"})
@NonNullApi
public abstract class JavaModuleDependenciesPlugin implements Plugin<Project> {

    private final Map<File, ModuleInfo> moduleInfo = new HashMap<>();
    private boolean warnForMissingCatalog;

    @Override
    public void apply(Project project) {
        if (GradleVersion.current().compareTo(GradleVersion.version("7.2")) < 0) {
            throw new GradleException("This plugin requires Gradle 7.2+");
        }

        project.getPlugins().apply(JavaPlugin.class);

        VersionCatalogsExtension versionCatalogs = project.getExtensions().findByType(VersionCatalogsExtension.class);
        warnForMissingCatalog = versionCatalogs == null;

        JavaModuleDependenciesExtension javaModuleDependenciesExtension = project.getExtensions().create(
                JAVA_MODULE_DEPENDENCIES, JavaModuleDependenciesExtension.class, versionCatalogs);
        javaModuleDependenciesExtension.getWarnForMissingVersions().convention(versionCatalogs != null);
        javaModuleDependenciesExtension.getVersionCatalogName().convention("libs");

        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        sourceSets.all(sourceSet ->  {
            process(ModuleInfo.Directive.REQUIRES, sourceSet.getImplementationConfigurationName(), sourceSet, project, javaModuleDependenciesExtension);
            process(ModuleInfo.Directive.REQUIRES_STATIC, sourceSet.getCompileOnlyConfigurationName(), sourceSet, project, javaModuleDependenciesExtension);
        });
        project.getPlugins().withType(JavaLibraryPlugin.class, p -> sourceSets.all(sourceSet ->  {
            process(ModuleInfo.Directive.REQUIRES_TRANSITIVE, sourceSet.getApiConfigurationName(), sourceSet, project, javaModuleDependenciesExtension);
            process(ModuleInfo.Directive.REQUIRES_STATIC_TRANSITIVE, sourceSet.getCompileOnlyApiConfigurationName(), sourceSet, project, javaModuleDependenciesExtension);
        }));

        setupReportTasks(project, javaModuleDependenciesExtension);
    }

    private void setupReportTasks(Project project, JavaModuleDependenciesExtension javaModuleDependencies) {
        project.getTasks().register("analyzeModulePath", AnalyzeModulePathReportTask.class, t -> {
            t.setGroup(HELP_GROUP);
            // t.setDescription("TODO");
        });
        project.getTasks().register("recommendModuleVersions", RecommendModuleVersionsReportTask.class, t -> {
            t.setGroup(HELP_GROUP);
            // t.setDescription("TODO");
        });
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

        Map<String, Object> gav = javaModuleDependencies.gav(moduleName);
        String projectName = ownModuleNamesPrefix == null ? null : moduleName.startsWith(ownModuleNamesPrefix + ".") ? moduleName.substring(ownModuleNamesPrefix.length() + 1) : null;

        if (!gav.isEmpty()) {
            project.getDependencies().add(configuration.getName(), gav);
            if (!gav.containsKey(GAV.VERSION)) {
                warnVersionMissing(moduleName, gav, moduleInfoFile, project, javaModuleDependencies);
            }
        } else if (projectName != null) {
            project.getDependencies().add(
                    configuration.getName(),
                    project.project(":" + projectName)
            );
        } else {
            throw new RuntimeException("No mapping registered for module: " + moduleDebugInfo(moduleName, moduleInfoFile, project.getRootDir()) +
                    " - use 'javaModuleDependencies.moduleNameToGA.put()' to add mapping.");
        }
    }

    private void warnVersionMissing(String moduleName, Map<String, Object> ga, Provider<RegularFile> moduleInfoFile, Project project, JavaModuleDependenciesExtension javaModuleDependencies) {
        if (warnForMissingCatalog) {
            project.getLogger().warn("[WARN] [Java Module Dependencies] Version catalog feature not enabled in settings.gradle(.kts) - add 'enableFeaturePreview(\"VERSION_CATALOGS\")'");
            warnForMissingCatalog = false;
        }

        if (javaModuleDependencies.getWarnForMissingVersions().forUseAtConfigurationTime().get()) {
            project.getLogger().warn("[WARN] [Java Module Dependencies] No version defined in catalog - " + ga.get(GAV.GROUP) + ":" + ga.get(GAV.ARTIFACT) + " - "
                    + moduleDebugInfo(moduleName.replace('.', '_'), moduleInfoFile, project.getRootDir()));
        }
    }

    private String moduleDebugInfo(String moduleName, Provider<RegularFile> moduleInfoFile, File rootDir) {
        return moduleName
                + " (required in "
                + moduleInfoFile.forUseAtConfigurationTime().get().getAsFile().getAbsolutePath().substring(rootDir.getAbsolutePath().length() + 1)
                + ")";
    }

}
