// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.tasks;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.component.ComponentSelector;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;
import org.gradlex.javamodule.dependencies.JavaModuleDependenciesExtension;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class ModuleVersionRecommendation extends DefaultTask {

    @Input
    public abstract ListProperty<String> getResolutionResult();

    @Input
    public abstract Property<Boolean> getPrintForPlatform();

    @Input
    public abstract Property<Boolean> getPrintForCatalog();

    @InputFile
    @Optional
    public abstract RegularFileProperty getPrintForPropertiesFile();

    @Inject
    public ModuleVersionRecommendation(Project project) {
        ConfigurationContainer configurations = project.getConfigurations();
        ComponentMetadataHandler components = project.getDependencies().getComponents();
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        JavaModuleDependenciesExtension javaModuleDependencies =
                project.getExtensions().getByType(JavaModuleDependenciesExtension.class);

        AttributeContainer rtClasspathAttributes = configurations
                .getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
                .getAttributes();
        Configuration latestVersionsClasspath = configurations.create("latestVersionsClasspath", c -> {
            c.setCanBeConsumed(false);
            c.setCanBeResolved(true);
            c.getAttributes()
                    .attribute(
                            Usage.USAGE_ATTRIBUTE,
                            Objects.requireNonNull(rtClasspathAttributes.getAttribute(Usage.USAGE_ATTRIBUTE)));
            c.getAttributes()
                    .attribute(
                            LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                            Objects.requireNonNull(
                                    rtClasspathAttributes.getAttribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE)));

            c.getResolutionStrategy().getDependencySubstitution().all(m -> {
                ComponentSelector requested = m.getRequested();
                if (requested instanceof ModuleComponentSelector) {
                    String group = ((ModuleComponentSelector) requested).getGroup();
                    String module = ((ModuleComponentSelector) requested).getModule();
                    m.useTarget(group + ":" + module + ":latest.release");
                }
            });
        });

        for (SourceSet sourceSet : sourceSets) {
            latestVersionsClasspath.extendsFrom(
                    configurations.getByName(sourceSet.getRuntimeClasspathConfigurationName()));
            latestVersionsClasspath.extendsFrom(
                    configurations.getByName(sourceSet.getCompileClasspathConfigurationName()));
        }

        components.all(c -> {
            String lcVersion = c.getId().getVersion().toLowerCase();
            if (lcVersion.contains("alpha")
                    || lcVersion.contains("-b")
                    || lcVersion.contains("beta")
                    || lcVersion.contains("cr")
                    || lcVersion.contains("ea")
                    || lcVersion.contains("m")
                    || lcVersion.contains("rc")) {

                c.setStatus("integration");
            }
        });
        getResolutionResult()
                .set(project.provider(
                        () -> latestVersionsClasspath.getIncoming().getResolutionResult().getAllComponents().stream()
                                .map(result -> {
                                    ModuleVersionIdentifier moduleVersion = result.getModuleVersion();
                                    if (moduleVersion != null
                                            && !(result.getId() instanceof ProjectComponentIdentifier)) {
                                        String ga = moduleVersion.getGroup() + ":" + moduleVersion.getName();
                                        String version = moduleVersion.getVersion();
                                        Provider<String> moduleName = javaModuleDependencies.moduleName(ga);
                                        if (moduleName.isPresent()) {
                                            return moduleName.get() + ":" + version;
                                        }
                                    }
                                    return null;
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList())));
    }

    @TaskAction
    public void report() {
        Set<String> moduleVersionsPlatform = new TreeSet<>();
        Set<String> moduleVersionsCatalog = new TreeSet<>();
        Set<String> moduleVersionsPropertiesFile = new TreeSet<>();

        for (String result : getResolutionResult().get()) {
            String moduleName = result.split(":")[0];
            String version = result.split(":")[1];
            moduleVersionsPlatform.add("    version(\"" + moduleName + "\", \"" + version + "\")");
            moduleVersionsCatalog.add(moduleName.replace('.', '_') + " = \"" + version + "\"");
            moduleVersionsPropertiesFile.add(moduleName + "=" + version);
        }

        if (getPrintForPlatform().get()) {
            p("");
            p("Latest Stable Versions of Java Modules - use in your platform project's build.gradle(.kts)");
            p("==========================================================================================");
            p("moduleInfo {");
            for (String entry : moduleVersionsPlatform) {
                p(entry);
            }
            p("}");
        }

        if (getPrintForCatalog().get()) {
            p("");
            p("Latest Stable Versions of Java Modules - use in [versions] section of 'gradle/libs.versions.toml'");
            p("=================================================================================================");
            for (String entry : moduleVersionsCatalog) {
                p(entry);
            }
        }

        if (getPrintForPropertiesFile().isPresent()) {
            p("");
            p("Latest Stable Versions of Java Modules - use in: "
                    + getPrintForPropertiesFile().get().getAsFile());
            p("=================================================================================================");
            for (String entry : moduleVersionsPropertiesFile) {
                p(entry);
            }
        }

        p("");
    }

    private void p(String toPrint) {
        System.out.println(toPrint);
    }
}
