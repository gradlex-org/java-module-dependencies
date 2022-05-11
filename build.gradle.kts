plugins {
    id("de.jjohannes.gradle-plugin")
    id("groovy")
}

group = "de.jjohannes.gradle"
version = "0.7"

dependencies {
    implementation("org.ow2.asm:asm:8.0.1")

    compileOnly("de.jjohannes.gradle:extra-java-module-info:0.12")

    testImplementation("org.spockframework:spock-core:2.1-groovy-3.0")
    testImplementation("org.gradle.exemplar:samples-check:1.0.0")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
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
