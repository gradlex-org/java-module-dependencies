pluginManagement {
    includeBuild("build-logic")
}
dependencyResolutionManagement {
    repositories.mavenCentral()
}

include("app")

enableFeaturePreview("VERSION_CATALOGS")