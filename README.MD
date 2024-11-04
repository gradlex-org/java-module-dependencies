# Java Module Dependencies Gradle plugin

[![Build Status](https://img.shields.io/endpoint.svg?url=https%3A%2F%2Factions-badge.atrox.dev%2Fgradlex-org%2Fjava-module-dependencies%2Fbadge%3Fref%3Dmain&style=flat)](https://actions-badge.atrox.dev/gradlex-org/java-module-dependencies/goto?ref=main)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v?label=Plugin%20Portal&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Forg%2Fgradlex%2Fjava-module-dependencies%2Forg.gradlex.java-module-dependencies.gradle.plugin%2Fmaven-metadata.xml)](https://plugins.gradle.org/plugin/org.gradlex.java-module-dependencies)

A Gradle plugin to make Gradle use dependencies from _module-info.java_ files automatically.
If you have a project that fully uses Java Modules, you do **not** need to declare dependencies in the `dependencies { }` block anymore.
Gradle will use the information from your `module-info.java` directly.

Minimal required Gradle version:
- **Gradle 7.4** if you **not** use the plugin in `settings.gradle.kts`
- **Gradle 8.8** to use the plugin in `settings.gradle.kts` and the [additional functionality](#project-structure-definition-when-using-this-plugin-as-settings-plugin) that comes with it.

To manage the versions of Java Modules, the plugin integrates with
[Platform Projects](https://docs.gradle.org/current/userguide/java_platform_plugin.html#sec:java_platform_usage) and 
[Dependency Versions Constraints](https://docs.gradle.org/current/userguide/dependency_constraints.html#sec:adding-constraints-transitive-deps) in general
as well as [Version Catalogs](https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog).

This [GradleX](https://gradlex.org) plugin is maintained by me, [Jendrik Johannes](https://github.com/jjohannes).
I offer consulting and training for Gradle and/or the Java Module System - please [reach out](mailto:jendrik.johannes@gmail.com) if you are interested.
There is also my [YouTube channel](https://www.youtube.com/playlist?list=PLWQK2ZdV4Yl2k2OmC_gsjDpdIBTN0qqkE) on Gradle topics.

If you have a suggestion or a question, please [open an issue](https://github.com/gradlex-org/java-module-dependencies/issues/new).

There is a [CHANGELOG.md](CHANGELOG.md).

# Java Modules with Gradle

If you build Java Modules with Gradle, you should consider using these plugins on top of Gradle core:

- [`id("org.gradlex.java-module-dependencies")`](https://github.com/gradlex-org/java-module-dependencies) (this plugin)
  Avoid duplicated dependency definitions and get your Module Path under control
- [`id("org.gradlex.java-module-testing")`](https://github.com/gradlex-org/java-module-testing)  
  Proper test setup for Java Modules
- [`id("org.gradlex.extra-java-module-info")`](https://github.com/gradlex-org/extra-java-module-info)  
  Only if you cannot avoid using non-module legacy Jars

[In episodes 31, 32, 33 of Understanding Gradle](https://github.com/jjohannes/understanding-gradle) I explain what these plugins do and why they are needed.
[<img src="https://onepiecesoftware.github.io/img/videos/31.png" width="260">](https://www.youtube.com/watch?v=X9u1taDwLSA&list=PLWQK2ZdV4Yl2k2OmC_gsjDpdIBTN0qqkE)
[<img src="https://onepiecesoftware.github.io/img/videos/32.png" width="260">](https://www.youtube.com/watch?v=T9U0BOlVc-c&list=PLWQK2ZdV4Yl2k2OmC_gsjDpdIBTN0qqkE)
[<img src="https://onepiecesoftware.github.io/img/videos/33.png" width="260">](https://www.youtube.com/watch?v=6rFEDcP8Noc&list=PLWQK2ZdV4Yl2k2OmC_gsjDpdIBTN0qqkE)

# How to use?

Working (example) projects to inspect:
- [java-module-system](https://github.com/jjohannes/java-module-system) contains a compact sample and further documentation
- [gradle-project-setup-howto](https://github.com/jjohannes/gradle-project-setup-howto/tree/java_module_system) is a full-fledged Java Module System project setup
- [hedera-services](https://github.com/hashgraph/hedera-services) is an open-source Java project using this plugin large scale

For general information about how to structure Gradle builds and apply community plugins like this one
you can check out my [Understanding Gradle video series](https://www.youtube.com/playlist?list=PLWQK2ZdV4Yl2k2OmC_gsjDpdIBTN0qqkE). 

## Plugin dependency

Add this to the build file of your convention plugin's build
(e.g. `gradle/plugins/build.gradle(.kts)` or `buildSrc/build.gradle(.kts)`).

```
dependencies {
  implementation("org.gradlex:java-module-dependencies:1.8")
}
```

> ⚠️ Due to [this bug](https://github.com/gradle/gradle/issues/21490#issuecomment-1458887481) in Gradle which may affect the plugin, it is recommented to add the following to your `settings.gradle(.kts)` file:
> ```
> includeBuild(".")
> ```

## Apply the plugin

The plugin can be used in two ways:

1. As _Settings Plugin_ in `settings.gradle(.kts)` file **(recommended)**
2. As _Project Plugin_ in `build.gradle(.kts)` files (sometimes easier to add to existing setups)

```
plugins {
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

## Project structure definition when using this plugin as Settings Plugin

The plugin offers a Gradle DSL extension to configure the location of _Java Modules_ in the project structure to be used
in the `settings.gradle(.kts)` file. It is an alternative to Gradle's native `include(...)` statement to configure
subprojects. The advantage of using this is that it is more compact than Gradle's `include(...)` and allows the plugin
to pick up more information during the [initialization phase](https://docs.gradle.org/current/userguide/build_lifecycle.html#sec:initialization).
By this, the plugin is later able to establish dependencies between your own modules without making assumptions about
how they need to be named (which is different when you use the plugin as
[Project Plugin](#project-structure-definition-when-using-this-plugin-as-project-plugin)).

```
// settings.gradle(.kts)
javaModules { // use instead of 'include(...)'
  module("module-a") // Module in directory, discovers 'src/*/java/module-info.java' files
  
  module("module-b") {
    group = "org.example"  // define group early so that all subprojects know all groups
    artifact = "lib-x"     // Gradle subproject name (if differnt than directory)
    plugin("java-library") // apply plugin to the Module's subproject to omit 'build.gradle'
  }

  directory("modules") {   // Auto-include all Modules in subfolders of 'modules' 
    group = "org.example"  // group for all Modules
    plugin("java-library") // apply plugin to all Modules' subprojects
    module("app") { ... }  // individualise Module (only if needed)
    
    // To optimze Configuration Cache hits:
    exclusions.add("_.*")  // do not inspect certain folders (regex)
    requiresBuildFile      // only look at folder containing a build.gradle(.kts)
  }
  
  versions("gradle/versions") // subproject configured as Platform Project
}
```

If you need more control over the properties of a Gradle subproject, in particular to define a nested project path,
you can still use Gradle's `include(...)` and then register the subproject with this plugin.

```
include(":project:with:custom:path")
javaModules { 
  module(project(":project:with:custom:path")) {
    group = "org.example"  // define group early so that all subprojects know all groups
    plugin("java-library") // apply plugin to the Module's subproject to omit 'build.gradle'
  }
}
```

## Project structure definition when using this plugin as Project Plugin

In this setup, subprojects with Java Modules are configured as in any traditional Gradle build: by using the
`include(...)` statement in `settings.gradle(.kts)`. The plugin is then applied in all subprojects with Java Modules,
ideally through a convention plugin. If you use the plugin like this, it needs to [make some assumption](#naming-patterns-for-modules-in-the-build-if-used-as-project-plugin) 
due to missing information and thus, for example, requires you to have the Gradle _project names_, _groups_ and _Java Module Names_ align.
The preferred way to use the plugin is to use it as [Settings Plugin](#project-structure-definition-when-using-this-plugin-as-settings-plugin).

## Define additional module dependencies in build files

With this plugin you move dependency definitions into `module-info.java` files and no longer use the `dependencies {}` block in build files.
However, there are certain dependency "scopes" not supported by the `module-info.java` syntax.
For this, the plugin offers an extension of Gradle's DSL to be used in `build.gradle(.kts)` files.

```
mainModuleInfo {
  runtimeOnly("org.slf4j.simple") // runtime only dependency for the 'main' module
  annotationProcessor("dagger.compiler")  // annotation processor dependency for the 'main' module
}
```

For modules in other source sets, there are corresponding blocks to define dependencies if needed – e.g. `testFixturesModuleInfo {}`.

In case a source set does **not** contain a `module-info.java`, all dependencies can be defined in the `build.gradle(.kts)` files.
The only case where this should be used is for whitebox testing activated via the [org.gradlex.java-module-testing](https://github.com/gradlex-org/java-module-testing) plugin.

```
testModuleInfo {
  requires("org.assertj.core")
  requires("org.hamcrest")
  requires("org.junit.jupiter.api")
}
```

## Add Module Name mapping information (if needed)

You may define additional mappings from _Module Name_ to _group:name (GA) coordinates_.

The plugin already knows about Modules available on Maven Central. The information is stored in:

- [modules.properties](src/main/resources/org/gradlex/javamodule/dependencies/modules.properties) - [please open a PR](https://github.com/gradlex-org/extra-java-module-info/pulls) if you miss an entry
- [unique_modules.properties](src/main/resources/org/gradlex/javamodule/dependencies/unique_modules.properties) - this information is extracted from [modules.properties](https://github.com/sormuras/modules/blob/main/com.github.sormuras.modules/com/github/sormuras/modules/modules.properties) by [@sormuras](https://github.com/sormuras)

You define additional entries (or overwrite entries from the plugin) in a `gradle/modules.properties` file in your project:

```
org.apache.commons.lang3=org.apache.commons:commons-lang3
org.apache.commons.lang3.test.fixtures=org.apache.commons:commons-lang3|test-fixtures
```

**Or** as part of the plugin configuration in your convention plugins:

```
javaModuleDependencies {
  // Module Name to Component GA Coordinates
  moduleNameToGA.put("org.apache.commons.lang3", "org.apache.commons:commons-lang3")

  // Module Name to Component GA Coordinates & Capability GA Coordinates
  moduleNameToGA.put("org.apache.commons.lang3.test.fixtures", "org.apache.commons:commons-lang3|test-fixtures")
}
```

There is also the option to register a mapping for all Modules that share a common _name prefix_ and _group_.
For example: `moduleNamePrefixToGroup.put("com.example.product.module.", "com.example.product")`.

## Naming patterns for Modules in the build (if used as Project Plugin)

This plugin makes the following assumption about _Module Names_ of your own Modules in the build to establish dependencies between them:

- **Module Name** == `"${prefixOfYourChoice}.${project.name}`

Or:

- **Module Name** == `"${prefixOfYourChoice}.${project.name}.${sourceSet.name}"`

A `project.name` is determined by the `include(projectName)` statement in the settings file.
A `sourceSet.name` is typically the name of the folder where the sources are located - e.g _main_ or _test_.

If you have a `prefixOfYourChoice`, all your Modules **need to have the same prefix** in order for the plugin to establish dependencies between the projects.

## Define Module versions in a Platform project as Dependency Constraints

Use Gradle's dependency constraints and/or platforms to define versions for the modules you depend on.
For that you can combine the `java-platform` with the `org.gradlex.java-module-versions` plugin which adds a `moduleInfo { }` configuration block.
In that block, you have the `version("module.name", "1.0")` notation to define a version by Module Name instead of coordinates.
For libraries that consist of multiple components and have a BOM for version management, you might prefer to include the BOM, which you need to do by coordinates, because a BOM does not have a Module Name.

```
plugins {
  id("java-platform")
  id("org.gradlex.java-module-versions")
}

// Define versions for Modules via the Module Name
moduleInfo {
  version("org.apache.xmlbeans", "5.0.1")
  version("org.slf4j", "2.0.7")
  version("org.slf4j.simple", "2.0.7")
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

org-junit-jupiter-api = "5.7.2"
```
- Note that the TOML notation does not support `.` as separater in the Module Names, but allows you to use `_` or `-` instead.
- _If_ you use a catalog with a custom name (not `libs`), you can tell the plugin using `versionCatalogName.set("customName")`.

## Find the latest stable version of a Module

The `recommendModuleVersions` help task prints the latest available versions of the Modules you require.

You may copy/paste the version constraints for your _platform project_ or _convention plugin_ from the task output:

```
$ ./gradlew :app:recommendModuleVersions -q

Latest Stable Versions of Java Modules - use in your platform project's build.gradle(.kts)
==========================================================================================
moduleInfo {
  version("com.fasterxml.jackson.annotation", "2.13.2")
  version("com.fasterxml.jackson.core", "2.13.2")
  version("com.fasterxml.jackson.databind", "2.13.2.2")
  version("org.apache.logging.log4j", "2.17.2")
  version("org.apache.xmlbeans", "5.0.3")
  version("org.junit.jupiter.api", "5.8.2")
  version("org.junit.jupiter.engine", "5.8.2")
  version("org.junit.platform.commons", "1.8.2")
  version("org.junit.platform.engine", "1.8.2")
  version("org.junit.platform.launcher", "1.8.2")
  version("org.opentest4j", "1.2.0")
  version("org.slf4j", "1.7.36")
  version("org.slf4j.simple", "1.7.36")
}
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
