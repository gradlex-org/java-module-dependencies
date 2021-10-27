plugins {
    id("org.my.gradle.java-module")
    id("application")
}

application {
    mainClass.set("org.my.app.App")
    mainModule.set("org.my.app")
}

dependencies.constraints {
    implementation(javaModuleDependencies.ga("com.fasterxml.jackson.databind")) {
        version { require("2.12.5") }
    }
}
