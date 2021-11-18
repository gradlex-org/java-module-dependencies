package de.jjohannes.gradle.moduledependencies;

import org.gradle.api.artifacts.VersionCatalog;
import org.gradle.api.artifacts.VersionCatalogsExtension;
import org.gradle.api.artifacts.VersionConstraint;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@SuppressWarnings("UnstableApiUsage")
public abstract class JavaModuleDependenciesExtension {
    public static String JAVA_MODULE_DEPENDENCIES = "javaModuleDependencies";

    private final VersionCatalogsExtension versionCatalogs;

    private Properties globalModuleNameToGA;

    public abstract MapProperty<String, String> getModuleNameToGA();

    public abstract Property<String> getOwnModuleNamesPrefix();

    public abstract Property<Boolean> getWarnForMissingVersions();

    public abstract Property<String> getVersionCatalogName();

    public JavaModuleDependenciesExtension(VersionCatalogsExtension versionCatalogs) {
        this.versionCatalogs = versionCatalogs;
    }

    public String ga(String moduleName) {
        Provider<String> customMapping = getModuleNameToGA().getting(moduleName);
        if (customMapping.isPresent()) {
            return customMapping.forUseAtConfigurationTime().get();
        } else {
            return (String) getGlobalModuleNameToGA().get(moduleName);
        }
    }

    public String gav(String moduleName) {
        String ga = ga(moduleName);
        String catalogName = getVersionCatalogName().forUseAtConfigurationTime().get();
        VersionCatalog catalog = versionCatalogs.named(catalogName);
        VersionConstraint version = catalog.findVersion(moduleName).orElse(null);
        if (version == null) {
            return ga;
        } else {
            return ga + ":" + version.getRequiredVersion();
        }
    }

    private Properties getGlobalModuleNameToGA() {
        if (this.globalModuleNameToGA != null) {
            return this.globalModuleNameToGA;
        }
        this.globalModuleNameToGA = new Properties();
        try (InputStream coordinatesFile = ModuleInfo.class.getResourceAsStream("modules.properties")) {
            this.globalModuleNameToGA.load(coordinatesFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this.globalModuleNameToGA;
    }
}
