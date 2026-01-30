// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.internal.utils;

import static org.gradlex.javamodule.dependencies.internal.utils.ModuleNamingUtil.sourceSetToModuleName;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleDirective;
import com.github.javaparser.ast.modules.ModuleProvidesDirective;
import com.github.javaparser.ast.modules.ModuleRequiresDirective;
import com.github.javaparser.ast.nodeTypes.NodeWithIdentifier;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class ModuleInfo implements Serializable {

    public enum Directive {
        REQUIRES,
        REQUIRES_TRANSITIVE,
        REQUIRES_STATIC,
        REQUIRES_STATIC_TRANSITIVE,
        REQUIRES_RUNTIME;

        public String literal() {
            return toString().toLowerCase().replace("_", " ").replace("runtime", RUNTIME_KEYWORD);
        }
    }

    public static final String RUNTIME_KEYWORD = "/*runtime*/";

    public static final ModuleInfo EMPTY = new ModuleInfo("");

    private final String moduleName;
    private final Map<String, String> imports;
    private final List<String> requires = new ArrayList<>();
    private final List<String> requiresTransitive = new ArrayList<>();
    private final List<String> requiresStatic = new ArrayList<>();
    private final List<String> requiresStaticTransitive = new ArrayList<>();
    private final List<String> requiresRuntime = new ArrayList<>();
    private final Map<String, List<String>> provides = new LinkedHashMap<>();

    public ModuleInfo(String moduleInfoFileContent) {
        Optional<CompilationUnit> result =
                new JavaParser().parse(moduleInfoFileContent).getResult();
        if (!result.isPresent() || !result.get().getModule().isPresent()) {
            moduleName = "";
            imports = Collections.emptyMap();
            return;
        }

        ModuleDeclaration moduleDeclaration = result.get().getModule().get();
        moduleName = moduleDeclaration.getNameAsString();
        imports = processImports(result.get());
        processDirectives(moduleDeclaration.getDirectives());
    }

    private Map<String, String> processImports(CompilationUnit cu) {
        return cu.getImports().stream()
                .map(NodeWithName::getName)
                .collect(Collectors.toMap(NodeWithIdentifier::getId, Node::toString));
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

    public Map<String, List<String>> getProvides() {
        return provides;
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
            throw new RuntimeException(
                    "Module name '" + moduleName + "' does not fit the project and source set names; "
                            + "expected name '<optional.prefix.>" + projectPlusSourceSetName + "'.");
        }
        return null;
    }

    private void processDirectives(List<ModuleDirective> directives) {
        for (ModuleDirective d : directives) {
            if (d instanceof ModuleRequiresDirective) {
                ModuleRequiresDirective directive = (ModuleRequiresDirective) d;
                String identifier = directive.getNameAsString();
                if (directive.isStatic() && directive.isTransitive()) {
                    requiresStaticTransitive.add(identifier);
                } else if (directive.isTransitive()) {
                    requiresTransitive.add(identifier);
                } else if (directive.isStatic()) {
                    requiresStatic.add(identifier);
                } else if (isRuntime(directive)) {
                    requiresRuntime.add(identifier);
                } else {
                    requires.add(identifier);
                }
            }
            if (d instanceof ModuleProvidesDirective) {
                ModuleProvidesDirective directive = (ModuleProvidesDirective) d;
                String name = qualifiedName(directive.getName());
                List<String> with = provides.computeIfAbsent(name, k -> new ArrayList<>());
                with.addAll(
                        directive.getWith().stream().map(this::qualifiedName).collect(Collectors.toList()));
            }
        }
    }

    private static boolean isRuntime(ModuleRequiresDirective directive) {
        return directive
                .getName()
                .getComment()
                .map(c -> "runtime".equals(c.getContent().trim()))
                .orElse(false);
    }

    private String qualifiedName(Name name) {
        if (imports.containsKey(name.getId())) {
            return imports.get(name.getId());
        } else {
            return name.toString();
        }
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
                moduleName, requires, requiresTransitive, requiresStatic, requiresStaticTransitive, requiresRuntime);
    }
}
