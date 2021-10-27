pluginManagement {
    includeBuild("build-logic")
}
dependencyResolutionManagement {
    repositories.mavenCentral()
}

include("app")

enableFeaturePreview("VERSION_CATALOGS")
dependencyResolutionManagement.versionCatalogs.create("modules") {
    version("org_apache_xmlbeans", "5.0.1")
    version("com_fasterxml_jackson_databind", "2.12.5")
}