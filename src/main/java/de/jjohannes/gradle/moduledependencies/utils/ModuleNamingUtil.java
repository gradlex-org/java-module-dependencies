package de.jjohannes.gradle.moduledependencies.utils;

import org.gradle.api.tasks.SourceSet;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ModuleNamingUtil {

    public static String sourceSetToModuleName(String projectName, String sourceSetName) {
        if (sourceSetName.equals(SourceSet.MAIN_SOURCE_SET_NAME)) {
            return toDottedCase(projectName);
        }
        return toDottedCase(projectName) + "." + toDottedCase(sourceSetName);
    }

    /**
     * Converts 'camelCase' and 'kebab-case' to 'dotted.case'.
     */
    private static String toDottedCase(String sourceSetName) {
        return Arrays.stream(sourceSetName.replace("-", ".")
                .split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])"))
                .map(String::toLowerCase).collect(Collectors.joining("."));
    }
}
