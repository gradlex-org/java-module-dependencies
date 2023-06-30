pluginManagement {
    includeBuild("gradle/plugins")
}
dependencyResolutionManagement {
    repositories.mavenCentral()
}

include("app", "lib")
