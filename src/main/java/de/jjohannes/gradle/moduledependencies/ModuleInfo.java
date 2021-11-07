package de.jjohannes.gradle.moduledependencies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ModuleInfo {

    public enum Directive {
        REQUIRES,
        REQUIRES_TRANSITIVE,
        REQUIRES_STATIC,
        REQUIRES_STATIC_TRANSITIVE
    }

    private final List<String> requires = new ArrayList<>();
    private final List<String> requiresTransitive = new ArrayList<>();
    private final List<String> requiresStatic = new ArrayList<>();
    private final List<String> requiresStaticTransitive = new ArrayList<>();

    public ModuleInfo(String moduleInfoFileContent) {
        for(String line: moduleInfoFileContent.split("\n")) {
            parse(line);
        }
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
        return Collections.emptyList();
    }

    private void parse(String moduleLine) {
        List<String> tokens = Arrays.asList(moduleLine.replace(";","").trim().split("\\s+"));
        if (tokens.size() > 1 && tokens.get(0).equals("requires")) {
            if (tokens.size() > 3 && tokens.contains("static") && tokens.contains("transitive")) {
                requiresStaticTransitive.add(tokens.get(3));
            } else if (tokens.size() > 2 && tokens.contains("transitive")) {
                requiresTransitive.add(tokens.get(2));
            } else if (tokens.size() > 2 && tokens.contains("static")) {
                requiresStatic.add(tokens.get(2));
            } else {
                requires.add(tokens.get(1));
            }
        }
    }
}
