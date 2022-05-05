pluginManagement {
    includeBuild("gradle-build-logic")
}

dependencyResolutionManagement {
    repositories.mavenCentral()
    repositories.gradlePluginPortal()
}

rootProject.name = "java-module-dependencies"
