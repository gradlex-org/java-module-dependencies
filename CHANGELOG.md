# Java Module Dependencies Gradle Plugin - Changelog

## Version 1.3
* [#22](https://github.com/gradlex-org/java-module-dependencies/issues/22) Add task to check ordering (alphabetical) of requires directives
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
