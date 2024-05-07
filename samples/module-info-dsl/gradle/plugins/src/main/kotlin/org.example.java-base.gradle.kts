plugins {
    id("java")
    id("org.gradlex.java-module-dependencies")
    id("org.gradlex.java-module-testing")
}

group = "org.example"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(11))
testing.suites.register<JvmTestSuite>("testFunctional")
tasks.check { dependsOn(tasks.named("testFunctional")) }

javaModuleDependencies {
    versionsFromPlatformAndConsistentResolution(":versions", ":app")
}

tasks.withType<Test>().configureEach {
    jvmArgs("-Dorg.slf4j.simpleLogger.defaultLogLevel=error")
}
