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
    implementation(platform(project(":platform")))
    javaModuleDependencies {
        testRuntimeOnly(ga("org.junit.jupiter.engine"))
        testRuntimeOnly(ga("org.junit.platform.launcher"))
    }
}
