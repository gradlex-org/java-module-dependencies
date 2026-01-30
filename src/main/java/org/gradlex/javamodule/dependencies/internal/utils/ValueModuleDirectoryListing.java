// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.internal.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class ValueModuleDirectoryListing
        implements ValueSource<List<String>, ValueModuleDirectoryListing.Parameter> {

    public interface Parameter extends ValueSourceParameters {
        Property<File> getDir();

        SetProperty<String> getExplicitlyConfiguredFolders();

        SetProperty<String> getExclusions();

        Property<Boolean> getRequiresBuildFile();
    }

    @Override
    public List<String> obtain() {
        Path path = getParameters().getDir().get().toPath();
        try (Stream<Path> directoryStream =
                Files.find(path, 1, (unused, basicFileAttributes) -> basicFileAttributes.isDirectory())) {
            return directoryStream
                    .filter(x -> !getParameters()
                            .getExplicitlyConfiguredFolders()
                            .get()
                            .contains(x.getFileName().toString()))
                    .filter(x -> getParameters().getExclusions().get().stream()
                            .noneMatch(r -> x.getFileName().toString().matches(r)))
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
