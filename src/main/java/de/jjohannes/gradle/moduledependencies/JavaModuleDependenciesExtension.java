package de.jjohannes.gradle.moduledependencies;

import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public abstract class JavaModuleDependenciesExtension {
    public static String JAVA_MODULE_DEPENDENCIES = "javaModuleDependencies";

    private Properties globalModuleNameToGA;

    public abstract MapProperty<String, String> getModuleNameToGA();

    public abstract Property<String> getOwnModuleNamesPrefix();

    public abstract Property<Boolean> getWarnForMissingVersions();

    public abstract Property<String> getVersionCatalogName();

    public String ga(String moduleName) {
        Provider<String> customMapping = getModuleNameToGA().getting(moduleName);
        if (customMapping.isPresent()) {
            return customMapping.forUseAtConfigurationTime().get();
        } else {
            return (String) getGlobalModuleNameToGA().get(moduleName);
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
