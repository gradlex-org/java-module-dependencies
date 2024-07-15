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

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Module {
    public abstract Property<String> getDirectory();
    public abstract Property<String> getArtifact();
    public abstract Property<String> getGroup();
    public abstract ListProperty<String> getModuleInfoPaths();
    public abstract ListProperty<String> getPlugins();

    @Inject
    public Module(File root) {
        getArtifact().convention(getDirectory().map(f -> Paths.get(f).getFileName().toString()));
        getModuleInfoPaths().convention(getDirectory().map(projectDir -> listChildren(root, projectDir + "/src")
                .map(srcDir -> new File(srcDir, "java/module-info.java"))
                .filter(File::exists)
                .map(moduleInfo -> "src/" + moduleInfo.getParentFile().getParentFile().getName() + "/java")
                .collect(Collectors.toList())));
    }

    private Stream<File> listChildren(File root, String projectDir) {
        File[] children = new File(root, projectDir).listFiles();
        return children == null ? Stream.empty() : Arrays.stream(children);
    }

    public void plugin(String id) {
        getPlugins().add(id);
    }
}
