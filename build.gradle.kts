plugins {
    id("gradlexbuild.module-mappings")
    id("org.gradlex.internal.plugin-publish-conventions") version "0.6"
}

group = "org.gradlex"
version = "1.10"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

tasks.compileJava {
    options.release = 8
}

tasks.withType<Javadoc>().configureEach {
    options {
        this as StandardJavadocDocletOptions
        encoding = "UTF-8"
        addStringOption("Xdoclint:all,-missing", "-quiet")
        addStringOption("Xwerror", "-quiet")
    }
}

configurations.compileClasspath {
    // Allow Java 11 dependencies on compile classpath
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 11)
}

dependencies {
    implementation("org.ow2.asm:asm:9.8")

    compileOnly("org.gradlex:extra-java-module-info:1.13.1")
    compileOnly("com.autonomousapps:dependency-analysis-gradle-plugin:3.0.4")

    testImplementation("org.assertj:assertj-core:3.27.6")
    testImplementation("org.gradle.exemplar:samples-check:1.0.3")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
}

pluginPublishConventions {
    id("${project.group}.${project.name}")
    implementationClass("org.gradlex.javamodule.dependencies.JavaModuleDependenciesPlugin")
    displayName("Java Module Dependencies Gradle Plugin")
    description("A plugin that makes Gradle respect the dependencies defined in 'module-info.java' files.")
    tags("gradlex", "java", "modularity", "jigsaw", "jpms", "dependencies", "versions")
    gitHub("https://github.com/gradlex-org/java-module-dependencies")
    developer {
        id.set("jjohannes")
        name.set("Jendrik Johannes")
        email.set("jendrik@gradlex.org")
    }
}

gradlePlugin.plugins.create("java-module-versions") {
    id = "${project.group}.${name}"
    implementationClass = "org.gradlex.javamodule.dependencies.JavaModuleVersionsPlugin"
    displayName = "Java Module Versions Gradle Plugin"
    description = "A plugin that makes Gradle respect the dependencies defined in 'module-info.java' files."
    tags = listOf("gradlex", "java", "modularity", "jigsaw", "jpms", "dependencies", "versions")
}

tasks.test {
    useJUnitPlatform()
    maxParallelForks = 4
    inputs.dir(layout.projectDirectory.dir("samples"))
}

testing.suites.named<JvmTestSuite>("test") {
    useJUnitJupiter()
    listOf("7.4", "7.6.5", "8.0.2", "8.14.3").forEach { gradleVersionUnderTest ->
        targets.register("test${gradleVersionUnderTest}") {
            testTask {
                group = LifecycleBasePlugin.VERIFICATION_GROUP
                description = "Runs tests against Gradle $gradleVersionUnderTest"
                systemProperty("gradleVersionUnderTest", gradleVersionUnderTest)
                exclude("**/*SamplesTest.class") // Not yet cross-version ready
                exclude("**/initialization/**") // Settings plugin only for Gradle 8.8+
                if (gradleVersionUnderTest == "7.4") {
                    // Configuration cache only "reliable" since 7.6 (?)
                    // https://github.com/gradlex-org/java-module-dependencies/issues/129
                    exclude("**/configcache/**")
                }
            }
        }
    }
    targets.all {
        testTask {
            maxParallelForks = 4
            inputs.dir(layout.projectDirectory.dir("samples"))
            inputs.dir("samples")
        }
    }
}
