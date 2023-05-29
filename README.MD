# Java Module Dependencies Gradle plugin

[![Build Status](https://img.shields.io/endpoint.svg?url=https%3A%2F%2Factions-badge.atrox.dev%2Fgradlex-org%2Fjava-module-dependencies%2Fbadge%3Fref%3Dmain&style=flat)](https://actions-badge.atrox.dev/gradlex-org/java-module-dependencies/goto?ref=main)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v?label=Plugin%20Portal&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Forg%2Fgradlex%2Fjava-module-dependencies%2Forg.gradlex.java-module-dependencies.gradle.plugin%2Fmaven-metadata.xml)](https://plugins.gradle.org/plugin/org.gradlex.java-module-dependencies)

A Gradle 7.4+ plugin to make Gradle use dependencies from _module-info.java_ files automatically.
If you have a project that fully uses Java Modules, you do **not** need to declare dependencies in the `dependencies { }` block anymore.
Gradle will use the information from your `module-info.java` directly.

To manage the versions of Java Modules, the plugin conveniently integrates with
[Platform Projects](https://docs.gradle.org/current/userguide/java_platform_plugin.html#sec:java_platform_usage) and 
[Dependency Versions Constraints](https://docs.gradle.org/current/userguide/dependency_constraints.html#sec:adding-constraints-transitive-deps) in general
as well as [Version Catalogs](https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog).

Here is an overview of what the plugin does in a Twitter thread:
[<img src="https://onepiecesoftware.github.io/img/tweets/1524322184923164672.png">](https://twitter.com/jeoj/status/1524322184923164672)

This [GradleX](https://gradlex.org) plugin is maintained by me, [Jendrik Johannes](https://github.com/jjohannes).
I offer consulting and training for Gradle and/or the Java Module System - please [reach out](mailto:jendrik.johannes@gmail.com) if you are interested.
There is also my [YouTube channel](https://www.youtube.com/playlist?list=PLWQK2ZdV4Yl2k2OmC_gsjDpdIBTN0qqkE) on Gradle topics.

If you have a suggestion or a question, please [open an issue](https://github.com/gradlex-org/java-module-dependencies/issues/new).

There is a [CHANGELOG.md](CHANGELOG.md).

# Java Modules with Gradle

If you plan to build Java Modules with Gradle, you should consider using these plugins on top of Gradle core:

- [`id("org.gradlex.java-module-dependencies")`](https://github.com/gradlex-org/java-module-dependencies)  
  Avoid duplicated dependency definitions and get your Module Path under control
- [`id("org.gradlex.java-module-testing")`](https://github.com/gradlex-org/java-module-testing)  
  Proper test setup for Java Modules
- [`id("org.gradlex.extra-java-module-info")`](https://github.com/gradlex-org/extra-java-module-info)  
  Only if your (existing) project cannot avoid using non-module legacy Jars

[Here is a sample](https://github.com/gradlex-org/java-module-testing/tree/main/samples/use-all-java-module-plugins)
that shows all plugins in combination.

[Full Java Module System Project Setup](https://github.com/jjohannes/gradle-project-setup-howto/tree/java_module_system) is a full-fledged Java Module System project setup using these plugins.  
[<img src="https://onepiecesoftware.github.io/img/videos/15-3.png" width="260">](https://www.youtube.com/watch?v=uRieSnovlVc&list=PLWQK2ZdV4Yl2k2OmC_gsjDpdIBTN0qqkE)

# How to use?

For a quick start, you can find some samples here:
* [samples/versions-in-platform](samples/versions-in-platform)
* [samples/versions-in-catalog](samples/versions-in-catalog)
* [samples/kotlin](samples/kotlin)

For general information about how to structure Gradle builds and apply community plugins like this one to all subprojects
you can check out my [Understanding Gradle video series](https://www.youtube.com/playlist?list=PLWQK2ZdV4Yl2k2OmC_gsjDpdIBTN0qqkE). 

## Plugin dependency

Add this to the build file of your convention plugin's build
(e.g. `gradle/plugins/build.gradle(.kts)` or `buildSrc/build.gradle(.kts)`).

```
dependencies {
    implementation("org.gradlex:java-module-dependencies:1.3")
}
```

## Apply the plugin

In your convention plugin, apply the plugin.

```
plugins {
    ...
    id("org.gradlex.java-module-dependencies")
}
```

## Effect of the plugin

Once the plugin is applied, dependencies are automatically determined based on the `requires` directives in your `module-info.java` files. For example:

```
module org.example.mymodule {
    requires com.fasterxml.jackson.core;   // -> implementation("com.fasterxml.jackson.core:jackson-core")
    requires transitive org.slf4j;         // -> api("org.slf4j:slf4j-api")
    requires static jakarta.servlet;       // -> compileOnly("jakarta.servlet:jakarta.servlet-api")
    requires /*runtime*/ org.slf4j.simple; // -> runtimeOnly("org.slf4j:slf4j-simple")
}
```

Note that `requires /*runtime*/` is a directive specifically supported by this plugin to allow the specification of _runtime only_ dependencies.

## Add Module Name mapping information (if needed)

You may define additional mappings from _Module Name_ to _group:name (GA) coordinates_.

The plugin already knows about Modules available on Maven Central. The information is stored in:

- [modules.properties](src/main/resources/org/gradlex/javamodule/dependencies/modules.properties) - [please open a PR](https://github.com/gradlex-org/extra-java-module-info/pulls) if you miss an entry
- [unique_modules.properties](src/main/resources/org/gradlex/javamodule/dependencies/unique_modules.properties) - this information is extracted from [modules.properties](https://github.com/sormuras/modules/blob/main/com.github.sormuras.modules/com/github/sormuras/modules/modules.properties) by [@sormuras](https://github.com/sormuras)

You can define additional entries (or overwrite entries from the plugin) as follows:

```
// optional configuration if required
javaModuleDependencies {
    // Make an automatic module known
    moduleNameToGA.put("org.apache.commons.lang3", "org.apache.commons:commons-lang3")
}
```

There is also the option to register a mapping for all Modules that share a common _name prefix_ and _group_.
For example: `moduleNamePrefixToGroup.put("com.example.product.module.", "com.example.product")`.

## Naming patterns for Modules in the build

This plugin makes the following assumption about _Module Names_ of your own Modules in the build to establish dependencies between them:

- **Module Name** == `"${prefixOfYourChoice}.${project.name}`

Or:

- **Module Name** == `"${prefixOfYourChoice}.${project.name}.${sourceSet.name}"`

A `project.name` is determined by the `include(projectName)` statement in the settings file.
A `sourceSet.name` is typically the name of the folder where the sources are located - e.g _main_ or _test_.

If you have a `prefixOfYourChoice`, all your Modules **need to have the same prefix** in order for the plugin to establish dependencies between the projects.

## Define Module versions in a Platform project as Dependency Constraints

Use Gradle's dependency constraints and/or platforms to define versions for the modules you depend on.
Inside a `javaModuleDependencies { }` block, you can use the `gav("module.name", "1.0")` notation to define a version by Module Name instead of coordinates.
For libraries that consist of multiple components and have a BOM for version management, you might prefer to include the BOM, which you need to do by coordinates, because a BOM does not have a Module Name.

```
plugins {
    id("java-platform")
    id("org.gradlex.java-module-dependencies")
}

// Define versions for Modules via the Module Name
dependencies.constraints {
    javaModuleDependencies {
        api(gav("org.apache.xmlbeans", "5.0.1"))
        api(gav("org.slf4j", "1.7.28"))
        api(gav("org.slf4j.simple", "1.7.28"))
    }
}

// Use BOMs for Modules that are part of a library of multiple Modules
javaPlatform.allowDependencies()
dependencies {
    api(platform("com.fasterxml.jackson:jackson-bom:2.13.2"))
    api(platform("org.junit:junit-bom:5.8.2"))
}
```

Note: If you need to declare additional dependencies without version, or want to use Gradle's rich versions, you can also use the `ga()` shortcut to map a Module Name to the corresponding GA coordinates.
For example:

```
dependencies {
    javaModuleDependencies {
        testRuntimeOnly(ga("org.junit.jupiter.engine"))
    }
}
```

## Define Module versions in a version catalog

Alternatively, versions can be defined in the `[version]` block of a [version catalog](https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog).
- *Note, if you use the _libs.versions.toml_ notation:* Since `.` is not supported, you need to use `_` as delimiter in the module names.

**settings.gradle.kts**

```
dependencyResolutionManagement {
    versionCatalogs.create("libs") {
        version("org.apache.xmlbeans", "5.0.1")
        version("com.fasterxml.jackson.databind", "2.12.5")
        version("org.slf4j", "2.0.7")
        
        version("org.junit.jupiter.api", "5.8.2")
    }
}
```

**gradle/libs.versions.toml**
```
[versions]
org_apache_xmlbeans = "5.0.1"
com_fasterxml_jackson_databind = "2.12.5"
org_slf4j = "2.0.7"

org_junit_jupiter_api = "5.7.2"
```

- _If_ you use a catalog, the plugin will warn if a version it looks for is missing.
  You can turn these warnings off using `warnForMissingVersions.set(false)`.
- _If_ you use a catalog with a custom name (not `libs`), you can tell the plugin using `versionCatalogName.set("customName")`.

## Find the latest stable version of a Module

The `recommendModuleVersions` help task prints the latest available versions of the Modules you require.

You may copy/paste the version constraints for your _platform project_ or _convention plugin_ from the task output:

```
$ ./gradlew :app:recommendModuleVersions -q

Latest Stable Versions of Java Modules - use in your platform project's build.gradle(.kts)
==========================================================================================
dependencies.constraints {
    javaModuleDependencies {
        api(gav("com.fasterxml.jackson.annotation", "2.13.2"))
        api(gav("com.fasterxml.jackson.core", "2.13.2"))
        api(gav("com.fasterxml.jackson.databind", "2.13.2.2"))
        api(gav("org.apache.logging.log4j", "2.17.2"))
        api(gav("org.apache.xmlbeans", "5.0.3"))
        api(gav("org.junit.jupiter.api", "5.8.2"))
        api(gav("org.junit.jupiter.engine", "5.8.2"))
        api(gav("org.junit.platform.commons", "1.8.2"))
        api(gav("org.junit.platform.engine", "1.8.2"))
        api(gav("org.junit.platform.launcher", "1.8.2"))
        api(gav("org.opentest4j", "1.2.0"))
        api(gav("org.slf4j", "1.7.36"))
        api(gav("org.slf4j.simple", "1.7.36"))
    }
}
```

Or entries for **gradle/libs.versions.toml** (in projects that use a version catalog):

```
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
```

## Check module-info.java files

You can use the `checkAllModuleInfo` check task to validate all `module-info.java` files for the following:

- Do the _requires_ directives correspond to what is needed in the source code?
  To activate this functionality, you need to apply the [com.autonomousapps.dependency-analysis](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin) in the root project.
- Are the _requires_ directives defined in alphabetical order?

## Analyze Module Paths

You can use the `moduleDependencies` and `analyzeModulePath` help task to analyse the Module Paths of a project.
It will show you which Modules are used and to which GAV coordinates they map.
It will also print potential issues - like Jars that are not Modules
(and are therefore put on the classpath)
or wrong custom mappings from Module Names to GAs where the Jars are not Modules.

```
$ ./gradlew :app:moduleDependencies --configuration=runtimeClasspath -q

------------------------------------------------------------
Project ':app'
------------------------------------------------------------

runtimeClasspath - Runtime classpath of source set 'main'.
+--- org.example.product.bespin
|    +--- org.example.product.corellia
|    |    +--- org.apache.poi.poi (5.2.2)
|    |    |    +--- org.apache.commons.codec (1.15)
|    |    |    +--- org.apache.commons.collections4 (4.4)
|    |    |    +--- commons.math3 (3.6.1)
|    |    |    +--- org.apache.commons.io (2.11.0)
|    |    |    +--- SparseBitSet (1.2)
|    |    |    \--- org.apache.logging.log4j (2.17.2)
|    |    +--- org.apache.commons.io (2.11.0)
|    |    \--- org.apache.poi.ooxml (5.2.2)
|    |         +--- org.apache.poi.poi (5.2.2) (*)
|    |         +--- org.apache.poi.ooxml.schemas (5.2.2)
|    |         |    \--- org.apache.xmlbeans (5.0.3)
|    |         |         \--- org.apache.logging.log4j (2.17.2)
|    |         +--- org.apache.xmlbeans (5.0.3) (*)
|    |         +--- org.apache.commons.compress (1.21)
|    |         +--- org.apache.commons.io (2.11.0)
|    |         +--- com.github.virtuald.curvesapi (1.07)
|    |         +--- org.apache.logging.log4j (2.17.2)
|    |         \--- org.apache.commons.collections4 (4.4)
|    +--- org.example.product.coruscant
|    |    +--- com.fasterxml.jackson.annotation (2.13.4)
|    |    +--- com.fasterxml.jackson.databind (2.13.4)
|    |    |    +--- com.fasterxml.jackson.annotation (2.13.4)
|    |    |    \--- com.fasterxml.jackson.core (2.13.4)
|    |    +--- com.google.common (30.1-jre)
|    |    +--- java.inject (1.0.5)
|    |    +--- com.fasterxml.jackson.core (2.13.4)
|    |    +--- com.fasterxml.jackson.datatype.jsr310 (2.13.4)
|    |    |    +--- com.fasterxml.jackson.annotation (2.13.4)
|    |    |    +--- com.fasterxml.jackson.core (2.13.4)
|    |    |    \--- com.fasterxml.jackson.databind (2.13.4) (*)
|    |    +--- jakarta.activation (1.2.2)
|    |    +--- jakarta.mail (1.6.7)
|    |    |    \--- jakarta.activation (1.2.2)
|    |    \--- org.slf4j (2.0.3)
|    \--- velocity.engine.core (2.3)
|         +--- org.apache.commons.lang3 (3.11)
|         \--- org.slf4j (2.0.3)
+--- org.example.product.corellia (*)
+--- org.example.product.kamino
|    \--- org.example.product.coruscant (*)
+--- org.example.product.kashyyyk
|    +--- org.example.product.naboo
|    +--- org.example.product.tatooine
|    +--- org.example.product.bespin (*)
|    \--- org.example.product.kamino (*)
+--- org.example.product.naboo
+--- org.example.product.tatooine
+--- jakarta.servlet (6.0.0)
\--- org.slf4j (2.0.3)
```


```
$ ./gradlew :app:analyzeModulePath -q

[INFO] All Java Modules required by this project
================================================
com.fasterxml.jackson.annotation -> com.fasterxml.jackson.core:jackson-annotations (2.13.2)
com.fasterxml.jackson.core -> com.fasterxml.jackson.core:jackson-core (2.13.2)
com.fasterxml.jackson.databind -> com.fasterxml.jackson.core:jackson-databind (2.13.2)
org.apache.logging.log4j -> org.apache.logging.log4j:log4j-api (2.14.0)
org.apache.xmlbeans -> org.apache.xmlbeans:xmlbeans (5.0.1)
org.apiguardian.api -> org.apiguardian:apiguardian-api (1.1.2)
org.junit.jupiter.api -> org.junit.jupiter:junit-jupiter-api (5.8.2)
org.junit.jupiter.engine -> org.junit.jupiter:junit-jupiter-engine (5.8.2)
org.junit.platform.commons -> org.junit.platform:junit-platform-commons (1.8.2)
org.junit.platform.engine -> org.junit.platform:junit-platform-engine (1.8.2)
org.junit.platform.launcher -> org.junit.platform:junit-platform-launcher (1.8.2)
org.my.app -> project :app
org.my.lib -> project :lib
org.opentest4j -> org.opentest4j:opentest4j (1.2.0)
org.slf4j -> org.slf4j:slf4j-api (1.7.28)
org.slf4j.simple -> org.slf4j:slf4j-simple (1.7.28)

[WARN] Components that are NOT Java Modules
===========================================
commons-cli:commons-cli (1.5.0)

Notes / Options:
  - This may be ok if you use the Classpath (aka ALL-UNNAMED) in addition to the Module Path (automatic modules can see ALL-UNNAMED)
  - Remove the dependencies or upgrade to higher versions
  - Patch legacy Jars to Modules: https://github.com/gradlex-org/extra-java-module-info
```

## Migrate existing Java projects to Java Modules

The plugin provides a `generateAllModuleInfoFiles` task for each project that applies it.
You can use that to generate an initial `module-info.java` from the dependencies declared in Gradle.
This is not a sophisticated migration tool, but useful, in combination with `analyzeModulePath`, to explore what it would take to migrate an existing project to Modules.


## Integration with the Extra Java Module Info plugin

This plugin integrates with the [Extra Java Module Info](https://github.com/gradlex-org/extra-java-module-info) plugin if both are applied.
Module Name mappings for Jars that were patched with extra module info will be automatically registered.

```
plugins {
    id("org.gradlex.extra-java-module-info")
    id("org.gradlex.java-module-dependencies")
}

extraJavaModuleInfo {
    automaticModule("org.apache.commons:commons-math3", "commons.math3")
    // Module Dependencies plugin automatically knows that 
    // 'commons.math3' now maps to 'org.apache.commons:commons-math3'
}
```

## Using Gradle's configuration cache

This plugin reads all your `module-info.java` files during build configuration.
This is, because they provide the additional dependency information for setting up the build correctly.
The files are rather small and we do not extract all the information from them (only the dependencies).
Therefore, it should not have much configuration time performance impact even on larger builds.

However, if you enable (the currently experimental) [configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html)
feature of Gradle, the result of the configuration phase is cached, avoiding parsing `module-info.java` files again in a successive build run.

```
org.gradle.configuration-cache=true
```

# Disclaimer

Gradle and the Gradle logo are trademarks of Gradle, Inc.
The GradleX project is not endorsed by, affiliated with, or associated with Gradle or Gradle, Inc. in any way.
