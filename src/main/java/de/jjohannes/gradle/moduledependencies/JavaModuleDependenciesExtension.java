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

/**
 * - Configure behavior of the 'java-module-dependencies' plugin
 * - Add additional mappings using {@link #getModuleNameToGA()}
 * - Define dependencies and dependency constraints by Module Name
 *   using {@link #ga(String)}, {@link #gav(String, String)} or {@link #gav(String)}
 */
public abstract class JavaModuleDependenciesExtension {
    static final String JAVA_MODULE_DEPENDENCIES = "javaModuleDependencies";

    private final VersionCatalogsExtension versionCatalogs;

    /**
     * @return the mappings from Module Name to GA coordinates; can be modified
     */
    public abstract MapProperty<String, String> getModuleNameToGA();

    /**
     * @return If a Version Catalog is used: print a WARN for missing versions (default is 'true')
     */
    public abstract Property<Boolean> getWarnForMissingVersions();

    /**
     * @return If a Version Catalog is used: which catalog? (default is 'libs')
     */
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
     *
     * @param moduleName The Module Name
     * @return Dependency notation
     */
    public Provider<String> ga(String moduleName) {
        return getModuleNameToGA().getting(moduleName);
    }

    /**
     * Converts 'Module Name' and 'Version' to GA coordinates that can be used in
     * dependency declarations as String: "group:name:version"
     *
     * @param moduleName The Module Name
     * @param version The (required) version
     * @return Dependency notation
     */
    public Provider<String> gav(String moduleName, String version) {
        return getModuleNameToGA().getting(moduleName).map(s -> s + ":" + version);
    }

    /**
     * If a Version Catalog is used:
     * Converts 'Module Name' and the matching 'Version' from the Version Catalog to
     * GAV coordinates that can be used in dependency Declarations as Map:
     * [group: "...", name: "...", version: "..."]
     *
     * @param moduleName The Module Name
     * @return Dependency notation
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

    /**
     * Finds the Module Name for given coordinates
     *
     * @param ga The GA coordinates
     * @return the first name found or 'null'
     */
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
