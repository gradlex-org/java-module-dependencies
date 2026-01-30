// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.internal.utils;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.gradle.api.tasks.SourceSet;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
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
        return Arrays.stream(sourceSetName.replace("-", ".").split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])"))
                .map(String::toLowerCase)
                .collect(Collectors.joining("."));
    }

    /**
     * Converts 'camelCase' to 'kebab-case'.
     */
    private static String toKebabCase(String sourceSetName) {
        return Arrays.stream(sourceSetName.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])"))
                .map(String::toLowerCase)
                .collect(Collectors.joining("-"));
    }
}
