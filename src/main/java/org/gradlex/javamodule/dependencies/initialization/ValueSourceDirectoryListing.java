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

import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ValueSourceDirectoryListing implements ValueSource<List<String>, ValueSourceDirectoryListing.DirectoryListingParameter> {


    @Override
    public List<String> obtain() {
        Path path = getParameters().getDir().get().toPath();
        File file = getParameters().getDir().get();
        try (Stream<Path> directoryStream = Files.find(path, 1, new BiPredicate<Path, BasicFileAttributes>() {
            @Override
            public boolean test(Path path, BasicFileAttributes basicFileAttributes) {
                return basicFileAttributes.isDirectory();
            }
        })) {
            return directoryStream.filter(x -> !getParameters().getExclusions().get().contains(x.getFileName().toString()))
                    .filter(x -> getParameters().getRegexExclusions().get().stream().noneMatch(r -> x.getFileName().toString().matches(r)))
                    .filter(x -> checkBuildFile(x, getParameters()))
                    .map(x -> x.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());

        } catch (IOException e) {
            throw new RuntimeException("Failed on " + file, e);
        }


    }

    private boolean checkBuildFile(Path x, DirectoryListingParameter parameters) {
        if (!parameters.getRequiresBuildFile().get()) {
            return true;
        }
        return Files.isRegularFile(x.resolve("build.gradle.kts")) || Files.isRegularFile(x.resolve("build.gradle"));
    }

    interface DirectoryListingParameter extends ValueSourceParameters {

        Property<Boolean> getRequiresBuildFile();

        Property<File> getDir();

        SetProperty<String> getExclusions();

        SetProperty<String> getRegexExclusions();

    }
}
