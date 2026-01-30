// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.initialization;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class Directory {

    private final File root;
    final Map<String, Module> customizedModules = new LinkedHashMap<>();

    /**
     * {@link Module#getGroup()}
     */
    public abstract Property<String> getGroup();

    /**
     * {@link Module#plugin(String)}
     */
    public abstract ListProperty<String> getPlugins();

    @Inject
    public Directory(File root) {
        this.root = root;
        getExclusions().convention(Arrays.asList("build", "\\..*"));
        getRequiresBuildFile().convention(false);
    }

    @Inject
    protected abstract ObjectFactory getObjects();

    /**
     * {@link Module#plugin(String)}
     */
    public void plugin(String id) {
        getPlugins().add(id);
    }

    /**
     * {@link Directory#module(String, Action)}
     */
    public void module(String subDirectory) {
        module(subDirectory, m -> {});
    }

    /**
     * Configure details of a Module in a subdirectory of this directory.
     * Note that Modules that are located in direct children of this directory are discovered automatically and
     * do not need to be explicitly mentioned.
     */
    public void module(String subDirectory, Action<Module> action) {
        Module module = addModule(subDirectory);
        action.execute(module);
        customizedModules.put(subDirectory, module);
    }

    Module addModule(String subDirectory) {
        Module module = getObjects().newInstance(Module.class, new File(root, subDirectory));
        module.getGroup().convention(getGroup());
        module.getPlugins().addAll(getPlugins());
        return module;
    }

    /**
     * Configure which folders should be ignored when searching for Modules.
     * This can be tweaked to optimize the configuration cache hit ratio.
     * Defaults to: 'build', '.*'
     */
    public abstract ListProperty<String> getExclusions();

    /**
     * Configure if only folders that contain a 'build.gradle' or 'build.gradle.kts'
     * should be considered when searching for Modules.
     * Setting this to true may improve configuration cache hit ratio if you know
     * that all modules have build files in addition to the 'module-info.java' files.
     */
    public abstract Property<Boolean> getRequiresBuildFile();
}
