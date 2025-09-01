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

import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.gradlex.javamodule.dependencies.internal.utils.ModuleNamingUtil.sourceSetToModuleName;

public class ModuleInfo implements Serializable {

    public enum Directive {
        REQUIRES,
        REQUIRES_TRANSITIVE,
        REQUIRES_STATIC,
        REQUIRES_STATIC_TRANSITIVE,
        REQUIRES_RUNTIME;

        public String literal() {
            return toString().toLowerCase().replace("_", " ")
                    .replace("runtime", RUNTIME_KEYWORD);
        }
    }

    public static final String RUNTIME_KEYWORD = "/*runtime*/";

    public static final ModuleInfo EMPTY = new ModuleInfo("");

    private String moduleName = "";
    private final List<String> requires = new ArrayList<>();
    private final List<String> requiresTransitive = new ArrayList<>();
    private final List<String> requiresStatic = new ArrayList<>();
    private final List<String> requiresStaticTransitive = new ArrayList<>();
    private final List<String> requiresRuntime = new ArrayList<>();


    public ModuleInfo(String moduleInfoFileContent) {
        boolean insideComment = false;
        for (String line : moduleInfoFileContent.split("\n")) {
            insideComment = parse(line, insideComment);
        }
    }

    public String getModuleName() {
        return moduleName;
    }

    public List<String> get(Directive directive) {
        if (directive == Directive.REQUIRES) {
            return requires;
        }
        if (directive == Directive.REQUIRES_TRANSITIVE) {
            return requiresTransitive;
        }
        if (directive == Directive.REQUIRES_STATIC) {
            return requiresStatic;
        }
        if (directive == Directive.REQUIRES_STATIC_TRANSITIVE) {
            return requiresStaticTransitive;
        }
        if (directive == Directive.REQUIRES_RUNTIME) {
            return requiresRuntime;
        }
        return Collections.emptyList();
    }

    @Nullable
    public String moduleNamePrefix(String projectName, String sourceSetName, boolean fail) {
        if (moduleName.equals(projectName)) {
            return "";
        }

        String projectPlusSourceSetName = sourceSetToModuleName(projectName, sourceSetName);
        if (moduleName.endsWith("." + projectPlusSourceSetName)) {
            return moduleName.substring(0, moduleName.length() - projectPlusSourceSetName.length() - 1);
        }
        if (moduleName.equals(projectPlusSourceSetName)) {
            return "";
        }
        if (moduleName.endsWith("." + projectName)) {
            return moduleName.substring(0, moduleName.length() - projectName.length() - 1);
        }
        if (this != EMPTY && fail) {
            throw new RuntimeException("Module name '" + moduleName + "' does not fit the project and source set names; " +
                    "expected name '<optional.prefix.>" + projectPlusSourceSetName + "'.");
        }
        return null;
    }

    /**
     * @return true, if we are inside a multi-line comment after this line
     */
    private boolean parse(String moduleLine, boolean insideComment) {
        if (insideComment) {
            return !moduleLine.contains("*/");
        }

        List<String> tokens = Arrays.asList(moduleLine
                .replace(";", "")
                .replace("{", "")
                .replace("}", "")
                .replace(RUNTIME_KEYWORD, "runtime")
                .replaceAll("/\\*.*?\\*/", " ")
                .trim().split("\\s+"));
        int singleLineCommentStartIndex = tokens.indexOf("//");
        if (singleLineCommentStartIndex >= 0) {
            tokens = tokens.subList(0, singleLineCommentStartIndex);
        }

        if (tokens.contains("module")) {
            moduleName = tokens.get(tokens.size() - 1);
        }
        if (tokens.size() > 1 && tokens.get(0).equals("requires")) {
            if (tokens.size() > 3 && tokens.contains("static") && tokens.contains("transitive")) {
                requiresStaticTransitive.add(tokens.get(3));
            } else if (tokens.size() > 2 && tokens.contains("transitive")) {
                requiresTransitive.add(tokens.get(2));
            } else if (tokens.size() > 2 && tokens.contains("static")) {
                requiresStatic.add(tokens.get(2));
            } else if (tokens.size() > 2 && tokens.contains("runtime")) {
                requiresRuntime.add(tokens.get(2));
            } else {
                requires.add(tokens.get(1));
            }
        }
        return moduleLine.lastIndexOf("/*") > moduleLine.lastIndexOf("*/");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModuleInfo that = (ModuleInfo) o;
        return Objects.equals(moduleName, that.moduleName)
                && Objects.equals(requires, that.requires)
                && Objects.equals(requiresTransitive, that.requiresTransitive)
                && Objects.equals(requiresStatic, that.requiresStatic)
                && Objects.equals(requiresStaticTransitive, that.requiresStaticTransitive)
                && Objects.equals(requiresRuntime, that.requiresRuntime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                moduleName,
                requires,
                requiresTransitive,
                requiresStatic,
                requiresStaticTransitive,
                requiresRuntime
        );
    }
}
