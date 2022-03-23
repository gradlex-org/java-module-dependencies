plugins {
    id("org.my.gradle.kotlin-java-module")
    id("application")
}

application {
    applicationDefaultJvmArgs = listOf("-ea")
    mainClass.set("org.my.app.AppKt")
    mainModule.set("org.my.app")
}

dependencies {
    runtimeOnly(javaModuleDependencies.gav("org.slf4j.simple"))
}
