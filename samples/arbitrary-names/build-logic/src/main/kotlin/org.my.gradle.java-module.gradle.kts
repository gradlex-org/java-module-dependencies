plugins {
    id("java")
    id("org.gradlex.java-module-dependencies")
}

group = "org.my"

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Dorg.slf4j.simpleLogger.defaultLogLevel=error")
}

javaModuleDependencies{
    moduleNameCheck = false

}

dependencies {
    javaModuleDependencies {
        testRuntimeOnly(gav("org.junit.jupiter.engine"))
        testRuntimeOnly(gav("org.junit.platform.launcher"))
    }
}
