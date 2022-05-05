plugins {
    id("de.jjohannes.gradle-plugin")
}

group = "de.jjohannes.gradle"
version = "0.6"

dependencies {
    implementation("org.ow2.asm:asm:8.0.1")

    compileOnly("de.jjohannes.gradle:extra-java-module-info:0.12")

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
