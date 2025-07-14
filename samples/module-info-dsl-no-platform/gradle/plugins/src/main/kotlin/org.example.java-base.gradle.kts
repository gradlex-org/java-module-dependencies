plugins {
    id("java")
    id("org.gradlex.java-module-dependencies")
    id("org.gradlex.java-module-testing")
    id("org.gradlex.jvm-dependency-conflict-resolution")
}

group = "org.example"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(11))
testing.suites.register<JvmTestSuite>("testFunctional")
tasks.check { dependsOn(tasks.named("testFunctional")) }

jvmDependencyConflicts {
    consistentResolution {
        providesVersions(":app")
    }
}
dependencies {
    implementation(platform(project(":app")) as ModuleDependency) {
        capabilities { requireCapability("${project.group}:app-platform") }
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("-Dorg.slf4j.simpleLogger.defaultLogLevel=error")
}
