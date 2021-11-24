package de.jjohannes.gradle.moduledependencies.tasks;

import de.jjohannes.gradle.moduledependencies.JavaModuleDependenciesExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.util.Set;
import java.util.TreeSet;

public abstract class ShowModuleNameMappingsReportTask extends DefaultTask {

    private final ConfigurationContainer configurations;
    private final SourceSetContainer sourceSets;
    private final JavaModuleDependenciesExtension javaModuleDependencies;

    @Inject
    public ShowModuleNameMappingsReportTask(Project project) {
        this.configurations = project.getConfigurations();
        this.sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        this.javaModuleDependencies = project.getExtensions().getByType(JavaModuleDependenciesExtension.class);
    }

    @TaskAction
    public void report() {
        Set<String> usedMappings = new TreeSet<>();
        Set<String> withoutMapping = new TreeSet<>();

        for (SourceSet sourceSet : sourceSets) {
            collect(configurations.getByName(sourceSet.getCompileClasspathConfigurationName()), usedMappings, withoutMapping);
            collect(configurations.getByName(sourceSet.getRuntimeClasspathConfigurationName()), usedMappings, withoutMapping);
        }

        p("");
        p("All Modules used on Runtime Classpath");
        p("=====================================");
        for (String entry : usedMappings) {
            p(entry);
        }

        if (!withoutMapping.isEmpty()) {
            p("");
            p("Components that are NOT Java Modules");
            p("=====================================");
            for (String entry : withoutMapping) {
                p(entry);
            }
        }
    }

    private void collect(Configuration configuration, Set<String> usedMappings, Set<String> withoutMapping) {
        for (ResolvedComponentResult result: configuration.getIncoming().getResolutionResult().getAllComponents()) {
            ModuleVersionIdentifier moduleVersion = result.getModuleVersion();
            if (moduleVersion != null) {
                String ga = moduleVersion.getGroup() + ":" + moduleVersion.getName();
                String version = " (" + moduleVersion.getVersion() + ")";
                String moduleName;
                if (result.getId() instanceof ProjectComponentIdentifier) {
                    moduleName = javaModuleDependencies.getOwnModuleNamesPrefix().get() + "." + moduleVersion.getName();
                } else {
                    moduleName = javaModuleDependencies.moduleName(ga);
                }
                if (moduleName == null) {
                    withoutMapping.add(ga + version);
                } else {
                    usedMappings.add(moduleName + " -> " + ga + version);
                }
            }
        }
    }

    private void p(String toPrint) {
        System.out.println(toPrint);
    }

}
