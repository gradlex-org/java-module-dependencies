plugins {
    id("java")
    id("org.gradlex.java-module-dependencies")
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
