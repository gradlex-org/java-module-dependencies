pluginManagement {
    includeBuild("build-logic")
}
dependencyResolutionManagement {
    repositories.mavenCentral()
}

include("app", "lib")

