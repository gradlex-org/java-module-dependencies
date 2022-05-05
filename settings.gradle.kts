pluginManagement {
    includeBuild("gradle-build-logic")
}
plugins {
    id("com.gradle.enterprise") version "3.10"
}

dependencyResolutionManagement {
    repositories.mavenCentral()
    repositories.gradlePluginPortal()
}

rootProject.name = "java-module-dependencies"

gradleEnterprise {
    buildScan {
        publishAlways()
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}
