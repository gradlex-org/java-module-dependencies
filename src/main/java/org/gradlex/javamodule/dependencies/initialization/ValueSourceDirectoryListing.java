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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ValueSourceDirectoryListing implements ValueSource<List<String>, ValueSourceDirectoryListing.DirectoryListingParameter> {


    @Override
    public List<String> obtain() {
        File file = getParameters().getDir().get();
        String[] list = file.list();
        if (list == null) {
            throw new RuntimeException("Failed to inspect: " + file.getAbsolutePath());
        }
        return Arrays.stream(list)
                .filter(x -> !getParameters().getExclusions().get().contains(x))
                .filter(x -> getParameters().getRegexExclusions().get().stream().noneMatch(x::matches))
                .sorted()
                .collect(Collectors.toList());


    }

    interface DirectoryListingParameter extends ValueSourceParameters {

        Property<File> getDir();

        SetProperty<String> getExclusions();

        SetProperty<String> getRegexExclusions();

    }
}
