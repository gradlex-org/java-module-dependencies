# Gradle Plugin: Java Module Dependencies 

A Gradle 7.4+ plugin to make Gradle use dependencies from _module-info.java_ files automatically.
If you have a project that fully uses Java Modules, you do **not** need to declare dependencies in the `dependencies {}` block anymore.
Gradle will use the information from your `module-info.java` directly.

Furthermore, you manage the versions of the module dependencies in a central place by employing the `[version]` block
of Gradle's new [version catalog](https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog) feature.

# How to use?

You can find a full example here: [samples/basic](samples/basic)

For general information about how to structure Gradle builds and apply community plugins like this one to all subprojects
you can check out my [Understanding Gradle video series](https://www.youtube.com/playlist?list=PLWQK2ZdV4Yl2k2OmC_gsjDpdIBTN0qqkE). 

## Plugin dependency

Add this to the build file of your convention plugin's build
(e.g. `build-logic/build.gradle(.kts)` or `buildSrc/build.gradle(.kts)`).

```
dependencies {
    implementation("de.jjohannes.gradle:java-module-dependencies:0.6")
}
```

## Apply the plugin

In your convention plugin, apply the plugin.

```
plugins {
    ...
    id("de.jjohannes.java-module-dependencies")
}
```

## Add Module Name mapping information (if needed)

You may define additional mappings from _module name_ to _group:name (GA) coordinates_.

The plugin already knows about modules available on Maven Central. The information is stored in:

- [modules.properties](src/main/resources/de/jjohannes/gradle/moduledependencies/modules.properties) - [please open a PR](https://github.com/jjohannes/extra-java-module-info/pulls) if you miss an entry
- [unique_modules.properties](src/main/resources/de/jjohannes/gradle/moduledependencies/unique_modules.properties) - this information is extracted from [modules.properties](https://github.com/sormuras/modules/blob/main/com.github.sormuras.modules/com/github/sormuras/modules/modules.properties) by [@sormuras](https://github.com/sormuras)

You can define additional entries (or overwrite entries from the plugin) as follows:

```
// optional configuration if required
javaModuleDependencies {
    // Make an automatic module known
    moduleNameToGA.put("org.apache.commons.lang3", "org.apache.commons:commons-lang3")
    // Do not warn if a module version is not know
    warnForMissingVersions.set(false)
}
```

## Naming patterns for Modules in the build

This plugin makes the following assumption about _Module Names_ of your own Modules in the build to establish dependencies between them:

- **Module Name** == `"${prefixOfYourChoice}.${project.name}`

Or:

- **Module Name** == `"${prefixOfYourChoice}.${project.name}.${sourceSet.name}"`

A `project.name` is determined by the `include(projectName)` statement in the settings file.
A `sourceSet.name` is typically the name of the folder where the sources are located - e.g _main_ or _test_.

If you have a `prefixOfYourChoice`, all your modules **need to have the same prefix** in order for the plugin to establish dependencies between the projects.

## Define Module versions

Versions are defined in `[version]` block of a dependencies [version catalog](https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog).
- *Note:* Since `.` is not supported, you need to use `_` as delimiter in the module names.

**gradle/libs.versions.toml**
```
[versions]
org_apache_xmlbeans = "5.0.1"
com_fasterxml_jackson_databind = "2.12.5"
org_slf4j = "1.7.32"

org_junit_jupiter_api = "5.7.2"
```

## Find the latest stable version of a Module

The `recommendModuleVersions` help task prints the latest available versions of the Modules you require.
You may copy/paste entries for **gradle/libs.versions.toml** from the task output:

````
$ ./gradlew :app:recommendModuleVersions -q

Latest Stable Versions of Java Modules - use in [versions] section of 'gradle/libs.versions.toml'
=================================================================================================
com_fasterxml_jackson_annotation = "2.13.2"
com_fasterxml_jackson_core = "2.13.2"
com_fasterxml_jackson_databind = "2.13.2"
org_apache_logging_log4j = "2.17.2"
org_apache_xmlbeans = "5.0.3"
org_junit_jupiter_api = "5.8.2"
org_junit_jupiter_engine = "5.8.2"
org_junit_platform_commons = "1.8.2"
org_junit_platform_engine = "1.8.2"
org_opentest4j = "1.2.0"
org_slf4j = "1.7.36"
org_slf4j_simple = "1.7.36"
````

## Custom version management

You may map a module name to the corresponding GA coordinates using `javaModuleDependencies.ga("my.module.name")`.
You can for example use this to define dependency versions by other means (e.g. dependency constraints).
If you want to do that, you can disable the warnings about missing versions and catalog by
`javaModuleDependencies.warnForMissingVersions.set(false)`.

## Analyze Module Pathes

You can use the `analyzeModulePath` help task to analyse the Module Pathes of a project.
It will show you which Modules are used and to which GAV coordinates they map.
It will also print potential issues - like Jars that are not Modules
(and are therefore put on the classpath)
or wrong custom mappings from Module Names to GAs where the Jars are not Modules.

```
$ ./gradlew :app:analyzeModulePath -q

All Java Modules required by this project
=========================================
com.fasterxml.jackson.annotation -> com.fasterxml.jackson.core:jackson-annotations (2.12.5)
com.fasterxml.jackson.core -> com.fasterxml.jackson.core:jackson-core (2.12.5)
com.fasterxml.jackson.databind -> com.fasterxml.jackson.core:jackson-databind (2.12.5)
org.apache.logging.log4j -> org.apache.logging.log4j:log4j-api (2.14.0)
org.apache.xmlbeans -> org.apache.xmlbeans:xmlbeans (5.0.1)
org.apiguardian.api -> org.apiguardian:apiguardian-api (1.1.0)
org.junit.jupiter.api -> org.junit.jupiter:junit-jupiter-api (5.7.2)
org.junit.jupiter.engine -> org.junit.jupiter:junit-jupiter-engine (5.7.2)
org.junit.platform.commons -> org.junit.platform:junit-platform-commons (1.7.2)
org.junit.platform.engine -> org.junit.platform:junit-platform-engine (1.7.2)
org.my.app -> project :app
org.my.lib -> project :lib
org.opentest4j -> org.opentest4j:opentest4j (1.2.0)
org.slf4j -> org.slf4j:slf4j-api (1.7.28)
org.slf4j.simple -> org.slf4j:slf4j-simple (1.7.28)

Components that are NOT Java Modules
====================================
commons-cli:commons-cli (1.5.0)
```

## Using Gradle's configuration cache

This plugin reads all your `module-info.java` files during build configuration.
This is, because they provide the additional dependency information for setting up the build correctly.
The files are rather small and we do not extract all the information from them (only the dependencies).
Therefore, it should not have much configuration time performance impact even on larger builds.

However, if you enable (the currently experimental) [configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html)
feature of Gradle, the result of the configuration phase is cached, avoiding parsing `module-info.java` files again in a successive build run.

```
org.gradle.unsafe.configuration-cache=true
```

