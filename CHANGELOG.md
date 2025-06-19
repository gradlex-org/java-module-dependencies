# Java Module Dependencies Gradle Plugin - Changelog

## Version 1.9.3
* [#209](https://github.com/gradlex-org/java-module-dependencies/issues/209) Fix: configuration cache issue when building kotlin-dsl plugins

## Version 1.9.2
* Update module name mappings

## Version 1.9.1
* Never attempt to create dependency for JDK core module
* Update and fix ([#199](https://github.com/gradlex-org/java-module-dependencies/pull/199)) module name mappings

## Version 1.9
* [#188](https://github.com/gradlex-org/java-module-dependencies/pull/188) Add `exportsTo` and `opensTo` statements to Module Info DSL

## Version 1.8.1
* Update module name mappings
* Update 'org.ow2.asm:asm' to 9.8

## Version 1.8
* [#136](https://github.com/gradlex-org/java-module-dependencies/pull/136) Support hierarchical project paths in Settings DSL
* [#141](https://github.com/gradlex-org/java-module-dependencies/pull/141) Introduce `org.gradlex.java-module-dependencies.register-help-tasks` property
* [#127](https://github.com/gradlex-org/java-module-dependencies/issues/127) Less configuration cache misses when modifying `module-info.java` (Thanks [TheGoesen](https://github.com/TheGoesen))
* [#128](https://github.com/gradlex-org/java-module-dependencies/issues/128) Less configuration cache misses when using Settings plugin (Thanks [TheGoesen](https://github.com/TheGoesen))
* [#135](https://github.com/gradlex-org/java-module-dependencies/issues/135) Improve performance of ApplyPluginsAction

## Version 1.7.1
* Update module name mappings

## Version 1.7
* [#112](https://github.com/gradlex-org/java-module-dependencies/issues/112) Settings plugin to configure module locations and identity

## Version 1.6.6
* [#113](https://github.com/gradlex-org/java-module-dependencies/issues/113) Fix: Do not fail for duplicated project names (Thanks [TheGoesen](https://github.com/TheGoesen))
* [#111](https://github.com/gradlex-org/java-module-dependencies/issues/111) Fix: Do not use 'MapProperty.unset' (Thanks [TheGoesen](https://github.com/TheGoesen))
* [#112](https://github.com/gradlex-org/java-module-dependencies/issues/112) Improve compatibility with Project Isolation

## Version 1.6.5
* [#104](https://github.com/gradlex-org/java-module-dependencies/issues/104) Fix: ModuleDependencyReport task does not correctly track inputs

## Version 1.6.4
* Enhance output of 'moduleDependencies' task
* Update 'org.ow2.asm:asm' to 9.7

## Version 1.6.3
* Update module name mappings

## Version 1.6.2
* [#90](https://github.com/gradlex-org/java-module-dependencies/issues/90) Fix: 'moduleNamePrefixToGroup' mapping uses best fit instead of first match
* [#91](https://github.com/gradlex-org/java-module-dependencies/issues/91) Fix: handle duplicated module names in 'extra-module-info' bridge

## Version 1.6.1
* Fix in setup of new utility tasks

## Version 1.6
* Add more utility tasks to migrate from/to module-info based dependencies
* Additional notation for module version DSL

## Version 1.5.2
* Fix for requires /*runtime*/ support

## Version 1.5.1
* Make `module-info.java` analysis tasks cacheable
* Make `recommendModuleVersions` configuration cache compatible
* Further tweak `requires /*runtime*/` support

## Version 1.5
* [#67](https://github.com/gradlex-org/java-module-dependencies/issues/67) Support local `modules.properties` for custom mappings
* [#65](https://github.com/gradlex-org/java-module-dependencies/issues/65) Error if a local Module Name does not match project name
* [#24](https://github.com/gradlex-org/java-module-dependencies/issues/24) Improve support for `requires /*runtime*/`

## Version 1.4.3
* Support '.' to '-' conversion in 'moduleNamePrefixToGroup'
* Fix issue in integration with 'extra-module-info'
* Improve support for Capability Coordinates in mappings
* Remove 'version missing in catalog' warning (triggered when catalog is used for different things)

## Version 1.4.2
* Fix Gradle 8.6 compatibility

## Version 1.4.1
* [#47](https://github.com/gradlex-org/java-module-dependencies/issues/47) Fix Gradle 8.3 compatibility

## Version 1.4
* [#31](https://github.com/gradlex-org/java-module-dependencies/issues/31) DSL for module dependencies that cannot be defined in module-info
* [#45](https://github.com/gradlex-org/java-module-dependencies/issues/45) Support Capability Coordinates in mappings

## Version 1.3.1
* Fix integration with analysis plugin if root projects are involved
* Fix in module name calculation for additional source sets
* Improve dependency analysis reporting for source sets without module-info.java 

## Version 1.3
* [#25](https://github.com/gradlex-org/java-module-dependencies/issues/25) Add 'moduleDependencies' help task - similar to 'dependencies' but with Module Names
* [#27](https://github.com/gradlex-org/java-module-dependencies/issues/27) Add task to check scopes of requires directives (by integrating with 'dependency-analysis' plugin)
* [#22](https://github.com/gradlex-org/java-module-dependencies/issues/22) Add task to check ordering (alphabetical) of requires directives
* [#29](https://github.com/gradlex-org/java-module-dependencies/issues/29) Add convenience to enable consistent resolution
* [#30](https://github.com/gradlex-org/java-module-dependencies/issues/30) Add an 'analyse only' mode
* [#23](https://github.com/gradlex-org/java-module-dependencies/issues/23) Consider 'moduleNamePrefixToGroup' entries in: ga(), gav(), moduleName()

## Version 1.2
* [#20](https://github.com/gradlex-org/java-module-dependencies/issues/20) Improve support for `requires /*runtime*/`

## Version 1.1
* [#19](https://github.com/gradlex-org/java-module-dependencies/issues/19) Support for `requires /*runtime*/`

## Version 1.0
* Moved project to [GradleX](https://gradlex.org) - new plugin ID: `org.gradlex.java-module-dependencies`

## Versions 0.11
* [#18](https://github.com/gradlex-org/java-module-dependencies/issues/18) Fix bug with single line comments in module-info.java 
* More mappings for Modules on Maven Central

## Versions 0.10
*  `moduleNamePrefixToGroup.put("..", "..")` to register mappings for a group of modules
* More mappings for Modules on Maven Central

## Versions 0.8
* More mappings for Modules on Maven Central

## Versions 0.1 - 0.7
* Initial features added
