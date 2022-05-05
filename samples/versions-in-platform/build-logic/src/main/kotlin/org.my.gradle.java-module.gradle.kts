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
    implementation(platform(project(":platform")))
    javaModuleDependencies {
        testRuntimeOnly(gav("org.junit.jupiter.engine"))
        testRuntimeOnly(gav("org.junit.platform.launcher")) // https://github.com/junit-team/junit5/issues/2730#issuecomment-942145712
    }
}
