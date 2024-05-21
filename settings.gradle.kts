pluginManagement {
    includeBuild("gradle/plugins")
}
plugins {
    id("com.gradle.develocity") version "3.17.4"
}

dependencyResolutionManagement {
    repositories.mavenCentral()
    repositories.gradlePluginPortal()
}

rootProject.name = "java-module-dependencies"

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"
    }
}
