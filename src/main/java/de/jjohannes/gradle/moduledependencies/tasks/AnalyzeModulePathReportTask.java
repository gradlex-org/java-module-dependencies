package de.jjohannes.gradle.moduledependencies.tasks;

import de.jjohannes.gradle.moduledependencies.JavaModuleDependenciesExtension;
import de.jjohannes.gradle.moduledependencies.ModuleInfo;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.jvm.JavaModuleDetector;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public abstract class AnalyzeModulePathReportTask extends DefaultTask {

    private final String projectName;
    private final SourceSetContainer sourceSets;
    private final JavaModuleDependenciesExtension javaModuleDependencies;

    @InputFiles
    public abstract ListProperty<Configuration> getClasspathConfigurations() ;

    @Inject
    public AnalyzeModulePathReportTask(Project project) {
        this.projectName = project.getName();
        this.sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        this.javaModuleDependencies = project.getExtensions().getByType(JavaModuleDependenciesExtension.class);
    }

    @Inject
    protected abstract JavaModuleDetector getJavaModuleDetector();

    @TaskAction
    public void report() throws IOException {
        Set<String> usedMappings = new TreeSet<>();
        Set<String> nonModules = new TreeSet<>();
        Set<String> missingMappings = new TreeSet<>();
        Set<String> wrongMappings = new TreeSet<>();

        SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        String ownModuleNamesPrefix = "";

        for (File folder : main.getJava().getSrcDirs()) {
            File file = new File(folder, "module-info.java");
            if (file.exists()) {
                String fileContent = Files.lines(file.toPath()).collect(Collectors.joining("\n"));
                ownModuleNamesPrefix = new ModuleInfo(fileContent).moduleNamePrefix(projectName, main.getName());
                break;
            }
        }

        for (Configuration classPath: getClasspathConfigurations().get()) {
            collect(classPath, usedMappings, nonModules, missingMappings, wrongMappings, ownModuleNamesPrefix);
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

    private void collect(Configuration configuration, Set<String> usedMappings, Set<String> nonModules, Set<String> missingMappings, Set<String> wrongMappings, String ownModuleNamesPrefix) {
        for (ResolvedArtifactResult result : configuration.getIncoming().getArtifacts()) {
            ComponentIdentifier id = result.getId().getComponentIdentifier();

            String moduleName;
            String version;
            String ga;

            if (id instanceof ProjectComponentIdentifier) {
                String projectName = ((ProjectComponentIdentifier) id).getProjectName();
                ga = id.getDisplayName();
                version = "";
                moduleName = ownModuleNamesPrefix + "." + projectName;
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
