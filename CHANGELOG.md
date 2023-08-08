# Java Module Dependencies Gradle Plugin - Changelog

## Version 1.4
* [#31](https://github.com/gradlex-org/java-module-dependencies/issues/31) DSL for module dependencies that cannot be defined in module-info

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
