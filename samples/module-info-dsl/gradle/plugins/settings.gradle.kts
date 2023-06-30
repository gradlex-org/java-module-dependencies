dependencyResolutionManagement {
    repositories.gradlePluginPortal()
}

// This is for testing against the latest version of the plugin, remove if you copied this for a real project
includeBuild(extra.properties["pluginLocation"] ?: rootDir.parentFile.parentFile.parentFile.parent)
