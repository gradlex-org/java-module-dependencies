pluginManagement {
    includeBuild("build-logic")
    includeBuild("../..")
}
dependencyResolutionManagement {
    includeBuild("../..")
    repositories.mavenCentral()
}

include("app", "lib")

