plugins {
    id("de.jjohannes.java-module-dependencies")
}

group = "org.my"

tasks.test {
    useJUnitPlatform()
}

dependencies {
    testRuntimeOnly(javaModuleDependencies.gav("org.junit.jupiter.engine"))
}
