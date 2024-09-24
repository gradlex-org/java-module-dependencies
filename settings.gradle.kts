pluginManagement {
    includeBuild("gradle/plugins")
}
plugins {
    id("com.gradle.develocity") version "3.18.1"
}

dependencyResolutionManagement {
    repositories.mavenCentral()
    repositories.gradlePluginPortal()
}

//includeBuild("samples/configuration-cache")

rootProject.name = "java-module-dependencies"

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"
    }
}
