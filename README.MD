# Gradle Plugin: Java Module Dependencies 

A Gradle 7.2+ plugin to make Gradle use dependencies from _module-info.java_ files automatically.
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
    implementation("de.jjohannes.gradle:java-module-dependencies:0.5")
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
The plugin already knows about all the _real_ modules available on Maven Central.
That [information](src/main/resources/de/jjohannes/gradle/moduledependencies/modules.properties)
is extracted from [modules.properties](https://github.com/sormuras/modules/blob/4eddb1225ee534e108075df1f1eed4b6f7ba1c5c/com.github.sormuras.modules/com/github/sormuras/modules/modules.properties)
by [@sormuras](https://github.com/sormuras).

```
// optional configuration if required
javaModuleDependencies {
    // Make an automatic module known
    moduleNameToGA.put("org.apache.commons.lang3", "org.apache.commons:commons-lang3")
    // Do not warn if a module version is not know
    warnForMissingVersions.set(false)
}
```

## Modules in the build (module name = name + . + group)

This plugin makes the following assumption about _Module Names_ of your own modules in the build to establish dependencies between them:

- **Module Name** == `"${project.name}.${project.group}"`

A project name is determined by the `include(projectName)` statement in the settings file.
The group is defined via the `group = ...` statement in the build files / convention plugin.
This corresponds to how GA coordinates are typically mapped to module names.

If you do not follow this pattern, you can also use a prefix that differs from the group:
`javaModuleDependencies.ownModuleNamesPrefix.set("org.my.module.name.prefix")`.

## Defining module versions

Versions are defined in `[version]` block of a dependencies [version catalog](https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog).
- *Note 1:* Since `.` is not supported, you need to use `_` as delimiter in the module names.
- *Note 2:* Version catalogs are a preview feature right now and you have to enable them via `enableFeaturePreview("VERSION_CATALOGS")` in _settings.gradle(.kts)_.

**gradle/libs.versions.toml**
```
[versions]
org_apache_xmlbeans = "5.0.1"
com_fasterxml_jackson_databind = "2.12.5"
org_slf4j = "1.7.32"

org_junit_jupiter_api = "5.7.2"
```

## Custom version management

You may map a module name to the corresponding GA coordinates using `javaModuleDependencies.ga("my.module.name")`.
You can for example use this to define dependency versions by other means (e.g. dependency constraints).
If you want to do that, you can disable the warnings about missing versions and catalog by
`javaModuleDependencies.warnForMissingVersions.set(false)`.

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

