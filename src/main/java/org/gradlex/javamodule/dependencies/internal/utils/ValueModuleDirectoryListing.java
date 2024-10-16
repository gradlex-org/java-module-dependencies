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

package org.gradlex.javamodule.dependencies.internal.utils;

import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ValueModuleDirectoryListing implements ValueSource<List<String>, ValueModuleDirectoryListing.Parameter> {

    public interface Parameter extends ValueSourceParameters {
        Property<File> getDir();
        SetProperty<String> getExplicitlyConfiguredFolders();
        SetProperty<String> getExclusions();
        Property<Boolean> getRequiresBuildFile();
    }

    @Override
    public List<String> obtain() {
        Path path = getParameters().getDir().get().toPath();
        try (Stream<Path> directoryStream = Files.find(path, 1, (unused, basicFileAttributes) -> basicFileAttributes.isDirectory())) {
            return directoryStream
                    .filter(x -> !getParameters().getExplicitlyConfiguredFolders().get().contains(x.getFileName().toString()))
                    .filter(x -> getParameters().getExclusions().get().stream().noneMatch(r -> x.getFileName().toString().matches(r)))
                    .filter(x -> checkBuildFile(x, getParameters()))
                    .map(x -> x.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());

        } catch (IOException e) {
            throw new RuntimeException("Failed to inspect: " + path, e);
        }
    }

    private boolean checkBuildFile(Path x, Parameter parameters) {
        if (!parameters.getRequiresBuildFile().get()) {
            return true;
        }
        return Files.isRegularFile(x.resolve("build.gradle.kts")) || Files.isRegularFile(x.resolve("build.gradle"));
    }
}
