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

import org.gradle.api.tasks.SourceSet;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class ModuleNamingUtil {

    public static String sourceSetToModuleName(String projectName, String sourceSetName) {
        if (sourceSetName.equals(SourceSet.MAIN_SOURCE_SET_NAME)) {
            return toDottedCase(projectName);
        }
        return toDottedCase(projectName) + "." + toDottedCase(sourceSetName);
    }

    @Nullable
    public static String sourceSetToCapabilitySuffix(String sourceSetName) {
        if (sourceSetName.equals(SourceSet.MAIN_SOURCE_SET_NAME)) {
            return null;
        }
        return toKebabCase(sourceSetName);
    }

    /**
     * Converts 'camelCase' and 'kebab-case' to 'dotted.case'.
     */
    private static String toDottedCase(String sourceSetName) {
        return Arrays.stream(sourceSetName.replace("-", ".")
                .split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])"))
                .map(String::toLowerCase).collect(Collectors.joining("."));
    }

    /**
     * Converts 'camelCase' to 'kebab-case'.
     */
    private static String toKebabCase(String sourceSetName) {
        return Arrays.stream(sourceSetName
                        .split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])"))
                .map(String::toLowerCase).collect(Collectors.joining("-"));
    }
}
