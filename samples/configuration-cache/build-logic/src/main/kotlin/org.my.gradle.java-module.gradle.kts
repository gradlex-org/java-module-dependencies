plugins {
    id("java")
    id("de.jjohannes.java-module-dependencies")
}

group = "org.my"

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Dorg.slf4j.simpleLogger.defaultLogLevel=error")
}

dependencies {
    javaModuleDependencies {
        testRuntimeOnly(gav("org.junit.jupiter.engine"))
    }
}
