plugins {
    id("org.my.gradle.java-module")
    id("application")
}

application {
    applicationDefaultJvmArgs = listOf("-ea")
    mainClass.set("org.my.app.App")
    mainModule.set("hokus.pokus")
}

dependencies {
    runtimeOnly(javaModuleDependencies.gav("org.slf4j.simple"))
}
