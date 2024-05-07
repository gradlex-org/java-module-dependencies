/*
 * Copyright the GradleX team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradlex.javamodule.dependencies;

import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Usage;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.util.GradleVersion;
import org.gradlex.javamodule.dependencies.dsl.ModuleVersions;
import org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo;
import org.gradlex.javamodule.dependencies.tasks.CatalogGenerate;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.gradle.api.attributes.Usage.JAVA_RUNTIME;
import static org.gradle.api.plugins.JavaPlatformPlugin.API_CONFIGURATION_NAME;
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES;
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES_RUNTIME;
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES_STATIC;
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES_STATIC_TRANSITIVE;
import static org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo.Directive.REQUIRES_TRANSITIVE;

@SuppressWarnings("unused")
@NonNullApi
public abstract class JavaModuleVersionsPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPlugins().withType(JavaPlatformPlugin.class, plugin -> setupForJavaPlatformProject(project));
        project.getPlugins().withType(JavaPlugin.class, plugin -> setupForJavaProject(project));
    }

    private void setupForJavaPlatformProject(Project project) {
        Configuration api = project.getConfigurations().getByName(API_CONFIGURATION_NAME);
        setupVersionsDSL(project, api);
        setupConstraintsValidation(project, api);
        registerCatalogTask(project);
    }

    private void setupForJavaProject(Project project) {
        ObjectFactory objects = project.getObjects();
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);

        Configuration versions = project.getConfigurations().create("versions", c -> {
            c.setCanBeResolved(false);
            c.setCanBeConsumed(false);
        });

        Configuration platformElements = project.getConfigurations().create("platformElements", c -> {
            c.setCanBeResolved(false);
            c.setCanBeConsumed(true);
            c.setVisible(false);
            c.extendsFrom(versions);
            c.getAttributes().attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, JAVA_RUNTIME));
        });

        if (GradleVersion.current().compareTo(GradleVersion.version("8.6")) < 0) {
            // https://github.com/gradle/gradle/issues/26163
            project.afterEvaluate(p -> platformElements.getOutgoing().capability(project.getGroup() + ":" + project.getName() + "-platform:" + project.getVersion()));
        } else {
            platformElements.getOutgoing().capability(project.provider(() -> project.getGroup() + ":" + project.getName() + "-platform:" + project.getVersion()));
        }

        setupVersionsDSL(project, versions);
        setupConstraintsValidation(project, versions);
        registerCatalogTask(project);
    }

    private void setupVersionsDSL(Project project, Configuration configuration) {
        project.getPlugins().apply(JavaModuleDependenciesPlugin.class);
        JavaModuleDependenciesExtension javaModuleDependencies = project.getExtensions().getByType(JavaModuleDependenciesExtension.class);
        project.getExtensions().create("moduleInfo", ModuleVersions.class, configuration, javaModuleDependencies);
    }

    private void setupConstraintsValidation(Project project, Configuration configuration) {
        configuration.getDependencyConstraints().configureEach(d -> {
            JavaModuleDependenciesExtension javaModuleDependencies = project.getExtensions().getByType(JavaModuleDependenciesExtension.class);
            String userDefinedReason = d.getReason();
            String ga = d.getModule().toString();
            Provider<String> moduleName = javaModuleDependencies.moduleName(ga);
            if (moduleName.isPresent() && isModuleName(userDefinedReason) && !moduleName.get().equals(userDefinedReason)) {
                project.getLogger().lifecycle("WARN: Expected module name for '" + ga + "' is '" + moduleName.get() + "' (not '" + userDefinedReason + "')");
            }
        });
    }

    private boolean isModuleName(@Nullable String s) {
        return s != null && !s.isEmpty() && !s.contains(" ");
    }

    private void registerCatalogTask(Project project) {
        JavaModuleDependenciesExtension javaModuleDependencies = project.getExtensions().getByType(JavaModuleDependenciesExtension.class);
        ModuleVersions moduleVersions = project.getExtensions().getByType(ModuleVersions.class);
        project.getTasks().register("generateCatalog", CatalogGenerate.class, t -> {
            t.setGroup("java modules");
            t.setDescription("Generate 'libs.versions.toml' file");

            t.getOwnProjectGroup().set(project.provider(() -> project.getGroup().toString()));

            t.getEntries().addAll(collectCatalogEntriesFromVersions(javaModuleDependencies, moduleVersions));
            project.getRootProject().getSubprojects().forEach(sub -> {
                File[] srcDirs = sub.getLayout().getProjectDirectory().dir("src").getAsFile().listFiles();
                (srcDirs == null ? Stream.<File>empty() : Arrays.stream(srcDirs)).forEach(srcDirSet -> {
                    File moduleInfoFile = new File(srcDirSet, "java/module-info.java");
                    if (!moduleInfoFile.exists()) {
                        moduleInfoFile = new File(srcDirSet, "java9/module-info.java");
                    }
                    if (moduleInfoFile.exists()) {
                        ModuleInfo moduleInfo = new ModuleInfo(project.getProviders().fileContents(project.getLayout().getProjectDirectory().file(moduleInfoFile.getAbsolutePath())).getAsText().get(), moduleInfoFile);
                        t.getEntries().addAll(collectCatalogEntriesFromModuleInfos(javaModuleDependencies, moduleInfo.get(REQUIRES_TRANSITIVE)));
                        t.getEntries().addAll(collectCatalogEntriesFromModuleInfos(javaModuleDependencies, moduleInfo.get(REQUIRES)));
                        t.getEntries().addAll(collectCatalogEntriesFromModuleInfos(javaModuleDependencies, moduleInfo.get(REQUIRES_STATIC_TRANSITIVE)));
                        t.getEntries().addAll(collectCatalogEntriesFromModuleInfos(javaModuleDependencies, moduleInfo.get(REQUIRES_STATIC)));
                        t.getEntries().addAll(collectCatalogEntriesFromModuleInfos(javaModuleDependencies, moduleInfo.get(REQUIRES_RUNTIME)));
                    }
                });
            });

            t.getEntries().addAll();

            t.getCatalogFile().set(project.getRootProject().getLayout().getProjectDirectory().file("gradle/libs.versions.toml"));
        });
    }

    private List<CatalogGenerate.CatalogEntry> collectCatalogEntriesFromVersions(JavaModuleDependenciesExtension javaModuleDependencies, ModuleVersions moduleVersions) {
        return moduleVersions.getDeclaredVersions().entrySet().stream().map(mv -> new CatalogGenerate.CatalogEntry(mv.getKey(), javaModuleDependencies.ga(mv.getKey()), mv.getValue())).collect(Collectors.toList());
    }

    private List<CatalogGenerate.CatalogEntry> collectCatalogEntriesFromModuleInfos(JavaModuleDependenciesExtension javaModuleDependencies, List<String> moduleNames) {
        return moduleNames.stream().map(moduleName -> new CatalogGenerate.CatalogEntry(moduleName, javaModuleDependencies.ga(moduleName), null)).collect(Collectors.toList());
    }

}
