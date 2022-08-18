plugins {
    // id("gradlexbuild.module-mappings")
    id("groovy")
    id("org.gradlex.internal.plugin-publish-conventions") version "0.4"
}

group = "de.jjohannes.gradle"
version = "0.12"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

signing {
    isRequired = false
}

dependencies {
    implementation("org.gradlex:${project.name}:1.0")

    testImplementation("org.spockframework:spock-core:2.1-groovy-3.0")
    testImplementation("org.gradle.exemplar:samples-check:1.0.0")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
}

pluginPublishConventions {
    id("de.jjohannes.${project.name}")
    implementationClass("de.jjohannes.gradle.moduledependencies.JavaModuleDependenciesPlugin")
    displayName("Java Module Dependencies Gradle Plugin")
    description("!!! Plugin ID changed to 'org.gradlex.${project.name}' !!!")
    tags("gradlex", "java", "modularity", "jigsaw", "jpms", "dependencies", "versions")
    gitHub("https://github.com/gradlex-org/java-module-dependencies")
    developer {
        id.set("jjohannes")
        name.set("Jendrik Johannes")
        email.set("jendrik@gradlex.org")
    }
}

tasks.test {
    useJUnitPlatform()
    maxParallelForks = 4
    inputs.dir(layout.projectDirectory.dir("samples"))
}
