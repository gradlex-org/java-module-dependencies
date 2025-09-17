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

package org.gradlex.javamodule.dependencies.test;

import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomizationTest {

    GradleBuild build = new GradleBuild();

    @Test
    void can_add_custom_mapping() {
        build.appBuildFile.appendText("""
            javaModuleDependencies {
                // Override because there are multiple alternatives
                moduleNameToGA.put("jakarta.mail", "com.sun.mail:jakarta.mail")
            }
            dependencies.constraints {
                javaModuleDependencies {
                    implementation(gav("jakarta.mail", "2.0.1"))
                }
            }""");
        build.appModuleInfoFile.appendText("""
            module org.gradlex.test.app {
                requires jakarta.mail;
            }""");

        var result = build.printRuntimeJars();

        assertThat(result.getOutput()).contains("[jakarta.mail-2.0.1.jar, jakarta.activation-2.0.1.jar]");
    }

    @Test
    void can_add_custom_mapping_via_properties_file_in_default_location() {
        var customModulesPropertiesFile = build.file("gradle/modules.properties");

        customModulesPropertiesFile.writeText("jakarta.mail=com.sun.mail:jakarta.mail");
        build.appBuildFile.appendText("moduleInfo { version(\"jakarta.mail\", \"2.0.1\") }");

        build.appModuleInfoFile.writeText("""
            module org.gradlex.test.app {
                requires jakarta.mail;
            }""");
        var result = build.printRuntimeJars();

        assertThat(result.getOutput()).contains("[jakarta.mail-2.0.1.jar, jakarta.activation-2.0.1.jar]");
    }

    @Test
    void can_add_custom_mapping_via_properties_file_in_custom_location() {
        var customModulesPropertiesFile = build.file(".hidden/modules.properties");

        customModulesPropertiesFile.writeText("jakarta.mail=com.sun.mail:jakarta.mail");
        build.appBuildFile.appendText("""
            moduleInfo { version("jakarta.mail", "2.0.1") }
            javaModuleDependencies {
                modulesProperties.set(File(rootDir,".hidden/modules.properties"))
            }""");

        build.appModuleInfoFile.writeText("""
            module org.gradlex.test.app {
                requires jakarta.mail;
            }""");

        var result = build.printRuntimeJars();

        assertThat(result.getOutput()).contains("[jakarta.mail-2.0.1.jar, jakarta.activation-2.0.1.jar]");
    }

    @Test
    void can_use_custom_catalog() {
        build.settingsFile.appendText("""
            dependencyResolutionManagement.versionCatalogs.create("moduleLibs") {
                version("org.apache.xmlbeans", "5.0.1")
                version("com.fasterxml.jackson.databind", "2.12.5")
            }""");
        build.appBuildFile.appendText("""
            javaModuleDependencies.versionCatalogName.set("moduleLibs")""");
        build.appModuleInfoFile.writeText("""
            module org.gradlex.test.app {
                requires com.fasterxml.jackson.databind;
                requires static org.apache.xmlbeans;
            }
        """);

        var runtime = build.printRuntimeJars();
        assertThat(runtime.getOutput()).contains("[jackson-annotations-2.12.5.jar, jackson-core-2.12.5.jar, jackson-databind-2.12.5.jar]");

        var compile = build.printCompileJars();
        assertThat(compile.getOutput()).contains("[xmlbeans-5.0.1.jar, jackson-annotations-2.12.5.jar, jackson-core-2.12.5.jar, jackson-databind-2.12.5.jar, log4j-api-2.14.0.jar]");
    }

    @Test
    void can_define_versions_in_platform_with_different_notations() {
        var customModulesPropertiesFile = build.file("gradle/modules.properties");

        customModulesPropertiesFile.writeText("jakarta.mail=com.sun.mail:jakarta.mail");
        build.appBuildFile.appendText("""
            moduleInfo {
                version("jakarta.mail", "2.0.1")
                version("jakarta.servlet", "6.0.0") { reject("[7.0.0,)") }
                version("java.inject") { require("1.0.5"); reject("[2.0.0,)") }
            }""");

        build.appModuleInfoFile.writeText("""
            module org.gradlex.test.app {
                requires jakarta.mail;
                requires jakarta.servlet;
                requires java.inject;
            }""");

        var result = build.printRuntimeJars();
        assertThat(result.getOutput()).contains("[jakarta.mail-2.0.1.jar, jakarta.servlet-api-6.0.0.jar, jakarta.inject-api-1.0.5.jar, jakarta.activation-2.0.1.jar]");
    }

    @Test
    void can_use_toml_catalog_with_underscore_for_dot() {
        build.file("gradle/libs.versions.toml").writeText("""
            [versions]
            org_apache_xmlbeans = "5.0.1"
            """);
        build.appModuleInfoFile.appendText("""
            module org.gradlex.test.app {
                requires org.apache.xmlbeans;
            }""");

        var result = build.build();

        assertThat(result.getOutput()).doesNotContain("[WARN]");
    }

    @Test
    void can_use_toml_catalog_with_dash_for_dot() {
        build.file("gradle/libs.versions.toml").writeText("""
            [versions]
            org-apache-xmlbeans = "5.0.1"
            """);
        build.appModuleInfoFile.appendText("""
            module org.gradlex.test.app {
                requires org.apache.xmlbeans;
            }""");

        var result = build.build();

        assertThat(result.getOutput()).doesNotContain("[WARN]");
    }
}
