plugins {
    id("de.jjohannes.java-module-dependencies")
}

group = "org.my"

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Dorg.slf4j.simpleLogger.defaultLogLevel=error")
}

dependencies {
    testRuntimeOnly(javaModuleDependencies.gav("org.junit.jupiter.engine"))
}
