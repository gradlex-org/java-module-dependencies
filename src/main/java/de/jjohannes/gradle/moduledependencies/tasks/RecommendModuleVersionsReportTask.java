package de.jjohannes.gradle.moduledependencies.tasks;

import de.jjohannes.gradle.moduledependencies.JavaModuleDependenciesExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.component.ComponentSelector;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public abstract class RecommendModuleVersionsReportTask extends DefaultTask {

    private final ConfigurationContainer configurations;
    private final ComponentMetadataHandler components;
    private final SourceSetContainer sourceSets;
    private final JavaModuleDependenciesExtension javaModuleDependencies;

    @Inject
    public RecommendModuleVersionsReportTask(Project project) {
        this.configurations = project.getConfigurations();
        this.components = project.getDependencies().getComponents();
        this.sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        this.javaModuleDependencies = project.getExtensions().getByType(JavaModuleDependenciesExtension.class);
    }

    @TaskAction
    public void report() {
        Set<String> moduleVersions = new TreeSet<>();
        AttributeContainer rtClasspathAttributes = configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME).getAttributes();
        Configuration latestVersionsClasspath = configurations.create("latestVersionsClasspath", c -> {
            c.setCanBeConsumed(false);
            c.setCanBeResolved(true);
            c.getAttributes().attribute(
                    Usage.USAGE_ATTRIBUTE,
                    Objects.requireNonNull(rtClasspathAttributes.getAttribute(Usage.USAGE_ATTRIBUTE)));
            c.getAttributes().attribute(
                    LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                    Objects.requireNonNull(rtClasspathAttributes.getAttribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE)));

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
            latestVersionsClasspath.extendsFrom(configurations.getByName(sourceSet.getRuntimeClasspathConfigurationName()));
            latestVersionsClasspath.extendsFrom(configurations.getByName(sourceSet.getCompileClasspathConfigurationName()));
        }

        components.all(c -> {
            String lcVersion = c.getId().getVersion().toLowerCase();
            if (lcVersion.contains("alpha")
                    || lcVersion.contains("-b")
                    || lcVersion.contains("beta")
                    || lcVersion.contains("cr")
                    || lcVersion.contains("m")
                    || lcVersion.contains("rc")) {

                c.setStatus("integration");
            }
        });

        for (ResolvedComponentResult result: latestVersionsClasspath.getIncoming().getResolutionResult().getAllComponents()) {
            ModuleVersionIdentifier moduleVersion = result.getModuleVersion();
            if (moduleVersion != null && !(result.getId() instanceof ProjectComponentIdentifier)) {
                String ga = moduleVersion.getGroup() + ":" + moduleVersion.getName();
                String version = moduleVersion.getVersion();
                String moduleName = javaModuleDependencies.moduleName(ga);
                if (moduleName != null) {
                    moduleVersions.add(moduleName.replace('.', '_') + " = \"" + version +"\"");
                }
            }
        }

        p("");
        p("Latest Stable Versions of Java Modules - use in [versions] section of 'gradle/libs.versions.toml'");
        p("=================================================================================================");
        for (String entry : moduleVersions) {
            p(entry);
        }
    }

    private void p(String toPrint) {
        System.out.println(toPrint);
    }

}
