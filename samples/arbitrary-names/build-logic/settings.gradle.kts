val value = extra.properties["pluginLocation"] ?: rootDir.parentFile.parentFile.parent
println(value)
includeBuild(value)
dependencyResolutionManagement {
    repositories.gradlePluginPortal()
}

// This is for testing against the latest version of the plugin, remove if you copied this for a real project



