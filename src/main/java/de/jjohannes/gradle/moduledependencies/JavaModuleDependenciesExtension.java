package de.jjohannes.gradle.moduledependencies;

import org.gradle.api.artifacts.VersionCatalog;
import org.gradle.api.artifacts.VersionCatalogsExtension;
import org.gradle.api.artifacts.VersionConstraint;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@SuppressWarnings("UnstableApiUsage")
public abstract class JavaModuleDependenciesExtension {
    public static final String JAVA_MODULE_DEPENDENCIES = "javaModuleDependencies";

    private final VersionCatalogsExtension versionCatalogs;

    private Map<String, String> globalModuleNameToGA;

    public abstract MapProperty<String, String> getModuleNameToGA();

    public abstract Property<Boolean> getWarnForMissingVersions();

    public abstract Property<String> getVersionCatalogName();

    public JavaModuleDependenciesExtension(VersionCatalogsExtension versionCatalogs) {
        this.versionCatalogs = versionCatalogs;
    }

    /**
     *  Converts Module Name to GA coordinates that can be used in dependency declarations as String:
     *  "group:name"
     */
    public String ga(String moduleName) {
        Provider<String> customMapping = getModuleNameToGA().getting(moduleName);
        if (customMapping.isPresent()) {
            return customMapping.forUseAtConfigurationTime().get();
        } else {
            return getGlobalModuleNameToGA().get(moduleName);
        }
    }

    /**
     *  Converts Module Name to GAV coordinates that can be used in dependency Declarations as Map:
     *  [group: "...", name: "...", version: "..."]
     *
     *  If no version is defined, the version entry will be missing.
     */
    public Map<String, Object> gav(String moduleName) {
        Map<String, Object> gav = new HashMap<>();
        String ga = ga(moduleName);
        if (ga == null) {
            return Collections.emptyMap();
        }

        VersionConstraint version = null;
        if (versionCatalogs != null) {
            String catalogName = getVersionCatalogName().forUseAtConfigurationTime().get();
            VersionCatalog catalog = versionCatalogs.named(catalogName);
            version = catalog.findVersion(moduleName.replace('_', '.')).orElse(null);
        }

        String[] gaSplit = ga.split(":");
        gav.put(GAV.GROUP, gaSplit[0]);
        gav.put(GAV.ARTIFACT, gaSplit[1]);
        if (version != null) {
            gav.put(GAV.VERSION, version);
        }

        return gav;
    }

    @Nullable
    public String moduleName(String ga) {
        for(Map.Entry<String, String> mapping: getModuleNameToGA().get().entrySet()) {
            if (mapping.getValue().equals(ga)) {
                return mapping.getKey();
            }
        }
        for(Map.Entry<String, String> mapping: getGlobalModuleNameToGA().entrySet()) {
            if (mapping.getValue().equals(ga)) {
                return mapping.getKey();
            }
        }
        return null;
    }

    public Map<String, String> getGlobalModuleNameToGA() {
        if (this.globalModuleNameToGA != null) {
            return this.globalModuleNameToGA;
        }
        Properties properties = new Properties() {
            @Override
            public synchronized Object put(Object key, Object value) {
                if (get(key) != null) {
                    throw new IllegalArgumentException(key + " already present.");
                }
                return super.put(key, value);
            }
        };
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Map<String, String> propertiesAsMap = (Map) properties;

        try (InputStream coordinatesFile = ModuleInfo.class.getResourceAsStream("unique_modules.properties")) {
            properties.load(coordinatesFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (InputStream coordinatesFile = ModuleInfo.class.getResourceAsStream("modules.properties")) {
            properties.load(coordinatesFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.globalModuleNameToGA = propertiesAsMap;
        return this.globalModuleNameToGA;
    }
}
