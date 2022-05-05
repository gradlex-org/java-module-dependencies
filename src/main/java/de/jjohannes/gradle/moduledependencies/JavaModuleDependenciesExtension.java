package de.jjohannes.gradle.moduledependencies;

import org.gradle.api.artifacts.VersionCatalog;
import org.gradle.api.artifacts.VersionCatalogsExtension;
import org.gradle.api.artifacts.VersionConstraint;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.empty;

public abstract class JavaModuleDependenciesExtension {
    static final String JAVA_MODULE_DEPENDENCIES = "javaModuleDependencies";

    private final VersionCatalogsExtension versionCatalogs;

    public abstract MapProperty<String, String> getModuleNameToGA();

    public abstract Property<Boolean> getWarnForMissingVersions();

    public abstract Property<String> getVersionCatalogName();

    public JavaModuleDependenciesExtension(VersionCatalogsExtension versionCatalogs) {
        this.versionCatalogs = versionCatalogs;
        getWarnForMissingVersions().convention(versionCatalogs != null);
        getVersionCatalogName().convention("libs");
        getModuleNameToGA().putAll(SharedMappings.mappings);
    }

    /**
     * Converts 'Module Name' to GA coordinates that can be used in
     * dependency declarations as String: "group:name"
     */
    public Provider<String> ga(String moduleName) {
        return getModuleNameToGA().getting(moduleName);
    }

    /**
     * Converts 'Module Name' and 'Version' to GA coordinates that can be used in
     * dependency declarations as String: "group:name:version"
     */
    public Provider<String> gav(String moduleName, String version) {
        return getModuleNameToGA().getting(moduleName).map(s -> s + ":" + version);
    }

    /**
     * Converts 'Module Name' and the matching 'Version' from the Version Catalog to
     * GAV coordinates that can be used in dependency Declarations as Map:
     * [group: "...", name: "...", version: "..."]
     */
    public Provider<Map<String, Object>> gav(String moduleName) {
        Provider<String> ga = ga(moduleName);

        VersionCatalog catalog = null;
        if (versionCatalogs != null) {
            String catalogName = getVersionCatalogName().get();
            catalog = versionCatalogs.named(catalogName);
        }
        Optional<VersionConstraint> version = catalog == null ? empty() : catalog.findVersion(moduleName.replace('_', '.'));

        return ga.map(s -> {
            Map<String, Object> gav = new HashMap<>();
            String[] gaSplit = s.split(":");
            gav.put(GAV.GROUP, gaSplit[0]);
            gav.put(GAV.ARTIFACT, gaSplit[1]);
            version.ifPresent(versionConstraint -> gav.put(GAV.VERSION, versionConstraint));
            return gav;
        });
    }

    @Nullable
    public String moduleName(String ga) {
        for(Map.Entry<String, String> mapping: getModuleNameToGA().get().entrySet()) {
            if (mapping.getValue().equals(ga)) {
                return mapping.getKey();
            }
        }
        return null;
    }
}
