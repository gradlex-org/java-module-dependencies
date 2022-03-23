plugins {
    id("de.jjohannes.java-module-dependencies")
    id("org.jetbrains.kotlin.jvm")
}

group = "org.my"
val moduleName = "${project.group}.${project.name}"
val testModuleName = "${moduleName}.test"

val javaLanguageVersion = JavaLanguageVersion.of(11)
java {
    toolchain.languageVersion.set(javaLanguageVersion)
}
kotlin.jvmToolchain {
    (this as JavaToolchainSpec).languageVersion.set(javaLanguageVersion)
}

// this is needed because we have a separate compile step in this example with the 'module-info.java' is in 'main/java' and the Kotlin code is in 'main/kotlin'
tasks.compileJava {
    // Compiling module-info in the 'main/java' folder needs to see already compiled Kotlin code
    options.compilerArgs = listOf("--patch-module", "$moduleName=${sourceSets.main.get().output.asPath}")
}

// Testing with JUnit5 (which is available in modules)
tasks.compileTestKotlin {
    // Make sure only module Jars are on the classpath and not the classes folders of the current project
    classpath = configurations.testCompileClasspath.get()
}
tasks.compileTestJava {
    // Compiling module-info in the 'test/java' folder needs to see already compiled Kotlin code
    options.compilerArgs = listOf("--patch-module", "$testModuleName=${sourceSets.test.get().output.asPath}")
    // Make sure only module Jars are on the classpath and not the classes folders of the current project
    classpath = configurations.testCompileClasspath.get()
}
val testJar = tasks.register<Jar>(sourceSets.test.get().jarTaskName) {
    // Package test code/resources as Jar so that they are a proper module at runtime
    archiveClassifier.set("tests")
    from(sourceSets.test.get().output)
}
tasks.test {
    classpath = configurations.testRuntimeClasspath.get() + files(testJar)
    useJUnitPlatform()
    jvmArgs("-Dorg.slf4j.simpleLogger.defaultLogLevel=error")
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    testRuntimeOnly(javaModuleDependencies.gav("org.junit.jupiter.engine"))
}
