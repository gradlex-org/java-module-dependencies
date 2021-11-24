package de.jjohannes.gradle.moduledependencies.tasks;

import de.jjohannes.gradle.moduledependencies.JavaModuleDependenciesExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.jvm.JavaModuleDetector;

import javax.inject.Inject;
import java.util.Set;
import java.util.TreeSet;

public abstract class AnalyzeModulePathReportTask extends DefaultTask {

    private final ConfigurationContainer configurations;
    private final SourceSetContainer sourceSets;
    private final JavaModuleDependenciesExtension javaModuleDependencies;

    @Inject
    public AnalyzeModulePathReportTask(Project project) {
        this.configurations = project.getConfigurations();
        this.sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        this.javaModuleDependencies = project.getExtensions().getByType(JavaModuleDependenciesExtension.class);
    }

    @Inject
    protected abstract JavaModuleDetector getJavaModuleDetector();

    @TaskAction
    public void report() {
        Set<String> usedMappings = new TreeSet<>();
        Set<String> nonModules = new TreeSet<>();
        Set<String> missingMappings = new TreeSet<>();
        Set<String> wrongMappings = new TreeSet<>();

        for (SourceSet sourceSet : sourceSets) {
            collect(configurations.getByName(sourceSet.getCompileClasspathConfigurationName()), usedMappings, nonModules, missingMappings, wrongMappings);
            collect(configurations.getByName(sourceSet.getRuntimeClasspathConfigurationName()), usedMappings, nonModules, missingMappings, wrongMappings);
        }

        p("");
        p("All Java Modules required by this project");
        p("=========================================");
        for (String entry : usedMappings) {
            p(entry);
        }

        if (!nonModules.isEmpty()) {
            p("");
            p("Components that are NOT Java Modules");
            p("====================================");
            for (String entry : nonModules) {
                p(entry);
            }
        }

        if (!missingMappings.isEmpty()) {
            p("");
            p("[WARN] Java Modules without Name Mapping");
            p("========================================");
            for (String entry : missingMappings) {
                p(entry);
            }
        }

        if (!wrongMappings.isEmpty()) {
            p("");
            p("[WARN] Wrong Mappings: Components are not Modules");
            p("=================================================");
            for (String entry : wrongMappings) {
                p(entry);
            }
        }
    }

    private void collect(Configuration configuration, Set<String> usedMappings, Set<String> nonModules, Set<String> missingMappings, Set<String> wrongMappings) {
        for (ResolvedArtifactResult result : configuration.getIncoming().getArtifacts()) {
            ComponentIdentifier id = result.getId().getComponentIdentifier();

            String moduleName;
            String version;
            String ga;

            if (id instanceof ProjectComponentIdentifier) {
                String projectName = ((ProjectComponentIdentifier) id).getProjectName();
                ga = id.getDisplayName();
                version = "";
                moduleName = javaModuleDependencies.getOwnModuleNamesPrefix().get() + "." + projectName;
            } else if (id instanceof ModuleComponentIdentifier){
                ModuleComponentIdentifier moduleVersion = (ModuleComponentIdentifier) id;
                ga = moduleVersion.getGroup() + ":" + moduleVersion.getModule();
                version = " (" + moduleVersion.getVersion() + ")";
                moduleName = javaModuleDependencies.moduleName(ga);
            } else {
                ga = "";
                version = "";
                moduleName = null;
            }

            boolean isModuleForReal = getJavaModuleDetector().isModule(true, result.getFile());

            if (moduleName != null && isModuleForReal) {
                usedMappings.add(moduleName + " -> " + ga + version);
            }
            if (moduleName == null && !isModuleForReal) {
                nonModules.add(ga + version);
            }
            if (moduleName == null && isModuleForReal) {
                missingMappings.add("??? -> " + ga + version);
            }
            if (moduleName != null && !isModuleForReal) {
                wrongMappings.add(moduleName + " -> " + ga + version);
            }
        }
    }

    private void p(String toPrint) {
        System.out.println(toPrint);
    }

}
