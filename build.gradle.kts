plugins {
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish") version "0.16.0"
}

group = "de.jjohannes.gradle"
version = "0.4"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

dependencies {
    testImplementation("org.gradle.exemplar:samples-check:1.0.0")
}

gradlePlugin {
    plugins {
        create(project.name) {
            id = "de.jjohannes.java-module-dependencies"
            implementationClass = "de.jjohannes.gradle.moduledependencies.JavaModuleDependenciesPlugin"
            displayName = "Java Module Dependencies"
            description = "A plugin that makes Gradle respect the dependencies defined in 'module-info.java' files."
        }
    }
}

pluginBundle {
    website = "https://github.com/jjohannes/java-module-dependencies"
    vcsUrl = "https://github.com/jjohannes/java-module-dependencies.git"
    tags = listOf("java", "modularity", "jigsaw", "jpms", "dependencies", "versions")
}

tasks.test {
    inputs.dir(layout.projectDirectory.dir("samples"))
}