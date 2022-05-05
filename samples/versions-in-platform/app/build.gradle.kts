plugins {
    id("org.my.gradle.java-module")
    id("application")
}

application {
    applicationDefaultJvmArgs = listOf("-ea")
    mainClass.set("org.my.app.App")
    mainModule.set("org.my.app")
}

dependencies {
    javaModuleDependencies {
        runtimeOnly(gav("org.slf4j.simple"))
    }
}
