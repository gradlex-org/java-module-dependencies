# Java Module Dependencies Gradle Plugin - Changelog

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
