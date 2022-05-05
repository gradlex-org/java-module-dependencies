package de.jjohannes.gradle.moduledependencies.tasks;

import de.jjohannes.gradle.moduledependencies.JavaModuleDependenciesExtension;
import de.jjohannes.gradle.moduledependencies.internal.utils.ModuleInfo;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.capabilities.Capability;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.jvm.JavaModuleDetector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public abstract class ModulePathAnalysis extends DefaultTask {
    private static final String AUTOMATIC_MODULE_NAME_ATTRIBUTE = "Automatic-Module-Name";
    private static final String MULTI_RELEASE_ATTRIBUTE = "Multi-Release";
    private static final String MODULE_INFO_CLASS_FILE = "module-info.class";
    private static final Pattern MODULE_INFO_CLASS_MRJAR_PATH = Pattern.compile("META-INF/versions/\\d+/module-info.class");

    private final String projectName;
    private final SourceSetContainer sourceSets;
    private final JavaModuleDependenciesExtension javaModuleDependencies;

    @InputFiles
    public abstract ListProperty<Configuration> getClasspathConfigurations() ;

    @Inject
    public ModulePathAnalysis(Project project) {
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
                try(Stream<String> lines = Files.lines(file.toPath())) {
                    String fileContent = lines.collect(Collectors.joining("\n"));
                    ownModuleNamesPrefix = new ModuleInfo(fileContent).moduleNamePrefix(projectName, main.getName());
                }
                break;
            }
        }

        for (Configuration classPath: getClasspathConfigurations().get()) {
            collect(classPath, usedMappings, nonModules, missingMappings, wrongMappings, ownModuleNamesPrefix);
        }

        p("");
        p("[INFO] All Java Modules required by this project");
        p("================================================");
        for (String entry : usedMappings) {
            p(entry);
        }

        if (!nonModules.isEmpty()) {
            p("");
            p("[WARN] Components that are NOT Java Modules");
            p("===========================================");
            for (String entry : nonModules) {
                p(entry);
            }
            p("");
            p("Notes / Options:");
            p("  - This may be ok if you use the Classpath (aka ALL-UNNAMED) in addition to the Module Path (automatic modules can see ALL-UNNAMED)");
            p("  - Remove the dependencies or upgrade to higher versions");
            p("  - Patch legacy Jars to Modules: https://github.com/jjohannes/extra-java-module-info");
        }

        if (!wrongMappings.isEmpty()) {
            p("");
            p("[WARN] Wrong Mappings: Components are not Modules");
            p("=================================================");
            for (String entry : wrongMappings) {
                p(entry);
            }
            p("");
            p("Options to fix:");
            p("  - Upgrade to newer version(s) - use ':recommendModuleVersions'");
            p("  - Fix wrong mapping, via 'moduleNameToGA.put('...', '...')'");
            p("  - If it is about a legacy Jar you want to use as Module, you need to patch it: https://github.com/jjohannes/extra-java-module-info");
            p("  - Report a wrong mapping in the plugin: https://github.com/jjohannes/java-module-dependencies/issues/new");
        }

        if (!missingMappings.isEmpty()) {
            p("");
            p("[WARN] Missing Mappings");
            p("=======================");
            p("");
            p("javaModuleDependencies {");
            for (String entry : missingMappings) {
                p("    " + entry);
            }
            p("}");
            p("");
            p("Options to fix:");
            p("  - Add mappings in your convention plugins - you may copy&paste the above output");
            p("  - Provide a PR to add missing mappings for well-known Modules to the plugin: https://github.com/jjohannes/java-module-dependencies/pulls");
        }
        p("");
    }

    private void collect(Configuration configuration, Set<String> usedMappings, Set<String> nonModules, Set<String> missingMappings, Set<String> wrongMappings, String ownModuleNamesPrefix) throws IOException {
        for (ResolvedArtifactResult result : configuration.getIncoming().getArtifacts()) {
            ComponentIdentifier id = result.getId().getComponentIdentifier();
            File resultFile = result.getFile();

            if (!resultFile.getName().endsWith(".jar") && !resultFile.getName().equals("classes")) {
                // Not an artifact with Java classes (e.g. resources folder of local project)
                continue;
            }

            String moduleName;
            String version;
            String ga;

            if (id instanceof ProjectComponentIdentifier) {
                String projectName = ((ProjectComponentIdentifier) id).getProjectName();
                ga = id.getDisplayName();
                version = "";
                List<Capability> capabilities = result.getVariant().getCapabilities();
                if (capabilities.isEmpty()) {
                    moduleName = ownModuleNamesPrefix + "." + projectName;
                } else {
                    moduleName = ownModuleNamesPrefix + "." + capabilities.get(0).getName().replace("-", ".");
                }
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

            boolean isModuleForReal = getJavaModuleDetector().isModule(true, resultFile);

            if (moduleName != null && isModuleForReal) {
                usedMappings.add(moduleName + " -> " + ga + version);
            }
            if (moduleName == null && !isModuleForReal) {
                nonModules.add(ga + version);
            }
            if (moduleName == null && isModuleForReal) {
                String actualModuleName = readNameFromModuleFromJarFile(resultFile);
                missingMappings.add("moduleNameToGA.put(\"" + actualModuleName + "\", \"" + ga + "\")");
            }
            if (moduleName != null && !isModuleForReal) {
                wrongMappings.add(moduleName + " -> " + ga + version);
            }
        }
    }

    private String readNameFromModuleFromJarFile(File jarFile) throws IOException {
        try (JarInputStream jarStream =  new JarInputStream(Files.newInputStream(jarFile.toPath()))) {
            String moduleName = getAutomaticModuleName(jarStream.getManifest());
            if (moduleName != null) {
                return moduleName;
            }
            boolean isMultiReleaseJar = containsMultiReleaseJarEntry(jarStream);
            ZipEntry next = jarStream.getNextEntry();
            while (next != null) {
                if (MODULE_INFO_CLASS_FILE.equals(next.getName())) {
                    return readNameFromModuleInfoClass(jarStream);
                }
                if (isMultiReleaseJar && MODULE_INFO_CLASS_MRJAR_PATH.matcher(next.getName()).matches()) {
                    return readNameFromModuleInfoClass(jarStream);
                }
                next = jarStream.getNextEntry();
            }
        }
        return null;
    }

    private String getAutomaticModuleName(Manifest manifest) {
        if (manifest == null) {
            return null;
        }
        return manifest.getMainAttributes().getValue(AUTOMATIC_MODULE_NAME_ATTRIBUTE);
    }

    private boolean containsMultiReleaseJarEntry(JarInputStream jarStream) {
        Manifest manifest = jarStream.getManifest();
        return manifest !=null && Boolean.parseBoolean(manifest.getMainAttributes().getValue(MULTI_RELEASE_ATTRIBUTE));
    }

    private String readNameFromModuleInfoClass(InputStream input) throws IOException {
        ClassReader classReader = new ClassReader(input);
        String[] moduleName = new String[1];
        classReader.accept(new ClassVisitor(Opcodes.ASM8) {
            @Override
            public ModuleVisitor visitModule(String name, int access, String version) {
                moduleName[0] = name;
                return super.visitModule(name, access, version);
            }
        }, 0);
        return moduleName[0];
    }

    private void p(String toPrint) {
        System.out.println(toPrint);
    }

}
