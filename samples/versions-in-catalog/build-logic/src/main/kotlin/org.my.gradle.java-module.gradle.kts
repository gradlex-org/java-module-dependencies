plugins {
    id("java")
    id("org.gradlex.java-module-dependencies")
}

group = "org.my"

javaModuleDependencies.versionsFromConsistentResolution(":app")

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Dorg.slf4j.simpleLogger.defaultLogLevel=error")
}

dependencies {
    javaModuleDependencies {
        testRuntimeOnly(gav("org.junit.jupiter.engine"))
        testRuntimeOnly(gav("org.junit.platform.launcher"))
    }
}
