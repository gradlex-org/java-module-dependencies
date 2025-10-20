pluginManagement {
    includeBuild("gradle/plugins")
}
plugins {
    id("com.gradle.develocity") version "4.2.2"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "java-module-dependencies"

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"
    }
}
