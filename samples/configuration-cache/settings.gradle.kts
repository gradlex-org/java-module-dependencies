pluginManagement {
    includeBuild("build-logic")
}
dependencyResolutionManagement {
    repositories.mavenCentral()
}

include("app", "lib")

enableFeaturePreview("VERSION_CATALOGS")
