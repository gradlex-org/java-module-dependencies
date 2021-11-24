pluginManagement {
    includeBuild("build-logic")
}
dependencyResolutionManagement {
    repositories.mavenCentral()
}

include("lib")

enableFeaturePreview("VERSION_CATALOGS")