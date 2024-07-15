package org.gradlex.javamodule.dependencies.initialization;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Modules {

    private final File root;
    final Map<String, Module> customizedModules = new LinkedHashMap<>();

    public abstract Property<String> getGroup();
    public abstract ListProperty<String> getPlugins();

    @Inject
    public abstract ObjectFactory getObjects();

    @Inject
    public Modules(File root) {
        this.root = root;
    }

    public void plugin(String id) {
        getPlugins().add(id);
    }

    public void module(String moduleFolder, Action<Module> action) {
        Module module = addModule(moduleFolder);
        action.execute(module);
        customizedModules.put(moduleFolder, module);
    }

    Module addModule(String moduleFolder) {
        Module module = getObjects().newInstance(Module.class, root);
        module.getFolder().convention(moduleFolder);
        module.getGroup().convention(getGroup());
        module.getPlugins().addAll(getPlugins());
        return module;
    }
}
