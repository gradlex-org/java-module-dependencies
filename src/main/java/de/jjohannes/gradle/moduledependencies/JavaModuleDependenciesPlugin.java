package de.jjohannes.gradle.moduledependencies;

import org.gradle.api.GradleException;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.VersionCatalog;
import org.gradle.api.artifacts.VersionCatalogsExtension;
import org.gradle.api.artifacts.VersionConstraint;
import org.gradle.api.file.RegularFile;
import org.gradle.api.logging.Logger;
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
    private boolean catalogFound = true;

    @Override
    public void apply(Project project) {
        if (GradleVersion.current().compareTo(GradleVersion.version("7.2")) < 0) {
            throw new GradleException("This plugin requires Gradle 7.2+");
        }

        project.getPlugins().apply(JavaPlugin.class);
        JavaModuleDependenciesExtension javaModuleDependenciesExtension = project.getExtensions().create(
                JAVA_MODULE_DEPENDENCIES, JavaModuleDependenciesExtension.class);
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
                    declareDependency(moduleName, project, configuration, javaModuleDependenciesExtension);
                }
            }
        }
    }

    private void declareDependency(String moduleName, Project project, Configuration configuration, JavaModuleDependenciesExtension javaModuleDependenciesExtension) {
        String ownGroup = project.getGroup().toString();
        String ga = javaModuleDependenciesExtension.ga(moduleName);
        String projectName = moduleName.startsWith(ownGroup + ".") ? moduleName.substring(ownGroup.length() + 1) : null;

        if (projectName != null) {
            project.getDependencies().add(
                    configuration.getName(),
                    project.project(":" + projectName)
            );
        } else if (ga != null) {
            project.getDependencies().add(
                    configuration.getName(),
                    toGAV(moduleName, ga, project, javaModuleDependenciesExtension)
            );
        }


    }

    private Map<String, Object> toGAV(String moduleName, String ga, Project project, JavaModuleDependenciesExtension javaModuleDependenciesExtension) {
        Map<String, Object> gav = new HashMap<>();

        VersionConstraint version = null;
        VersionCatalogsExtension versionCatalogs = project.getExtensions().findByType(VersionCatalogsExtension.class);
        if (versionCatalogs == null) {
            warnVersionMissing(project.getLogger(), javaModuleDependenciesExtension, "Version catalog feature not enabled in settings.gradle(.kts) - add 'enableFeaturePreview(\"VERSION_CATALOGS\")'");
            catalogFound = false;
        } else {
            String catalogName = javaModuleDependenciesExtension.getVersionCatalogName().forUseAtConfigurationTime().get();
            VersionCatalog catalog = versionCatalogs.named(catalogName);
            version = catalog.findVersion(moduleName).orElse(null);
            if (version == null) {
                warnVersionMissing(project.getLogger(), javaModuleDependenciesExtension, "No version defined in catalog - " + moduleName.replace('.', '_'));
            }
        }

        String[] gaSplit = ga.split(":");
        gav.put("group", gaSplit[0]);
        gav.put("name", gaSplit[1]);
        if (version != null) {
            gav.put("version", version);
        }

        return gav;
    }

    private void warnVersionMissing(Logger logger, JavaModuleDependenciesExtension javaModuleDependenciesExtension, String message) {
        if (catalogFound && javaModuleDependenciesExtension.getWarnForMissingVersions().forUseAtConfigurationTime().get()) {
            logger.warn("[WARN] [Java Module Dependencies] " + message);
        }
    }
}
