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
