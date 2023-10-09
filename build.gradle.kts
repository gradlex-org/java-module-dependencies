plugins {
    id("gradlexbuild.module-mappings")
    id("groovy")
    id("org.gradlex.internal.plugin-publish-conventions") version "0.6"
}

group = "org.gradlex"
version = "1.4.1"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

configurations.compileClasspath {
    // Allow Java 11 dependencies on compile classpath
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 11)
}

dependencies {
    implementation("org.ow2.asm:asm:9.6")

    compileOnly("org.gradlex:extra-java-module-info:1.4.1")
    compileOnly("com.autonomousapps:dependency-analysis-gradle-plugin:1.25.0")

    testImplementation("org.spockframework:spock-core:2.1-groovy-3.0")
    testImplementation("org.gradle.exemplar:samples-check:1.0.2")
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
