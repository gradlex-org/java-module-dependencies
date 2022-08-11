plugins {
    id("de.jjohannes.gradle-plugin")
    id("groovy")
}

group = "de.jjohannes.gradle"
version = "0.11"

dependencies {
    implementation("org.ow2.asm:asm:8.0.1")

    compileOnly("de.jjohannes.gradle:extra-java-module-info:0.13")

    testImplementation("org.spockframework:spock-core:2.1-groovy-3.0")
    testImplementation("org.gradle.exemplar:samples-check:1.0.0")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
}

val pluginId = "de.jjohannes.java-module-dependencies"
val pluginClass = "de.jjohannes.gradle.moduledependencies.JavaModuleDependenciesPlugin"
val pluginName = "Java Module Dependencies Gradle Plugin"
val pluginDescription = "A plugin that makes Gradle respect the dependencies defined in 'module-info.java' files."
val pluginBundleTags = listOf("java", "modularity", "jigsaw", "jpms", "dependencies", "versions")
val pluginGitHub = "https://github.com/jjohannes/java-module-dependencies"

gradlePlugin {
    plugins {
        create(project.name) {
            id = pluginId
            implementationClass = pluginClass
            displayName = pluginName
            description = pluginDescription
        }
    }
}

pluginBundle {
    website = pluginGitHub
    vcsUrl = pluginGitHub
    tags = pluginBundleTags
}

publishing {
    publications.withType<MavenPublication>().all {
        pom.name.set(pluginName)
        pom.description.set(pluginDescription)
        pom.url.set(pluginGitHub)
        pom.licenses {
            license {
                name.set("Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        pom.developers {
            developer {
                id.set("jjohannes")
                name.set("Jendrik Johannes")
                email.set("jendrik@onepiece.software")
            }
        }
        pom.scm {
            url.set(pluginGitHub)
        }
    }
}
