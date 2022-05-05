import de.jjohannes.gradle.moduledependencies.gradlebuild.tasks.UniqueModulesPropertiesUpdate

plugins {
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

tasks.test {
    inputs.dir(layout.projectDirectory.dir("samples"))
}

val updateUniqueModulesProperties = tasks.register<UniqueModulesPropertiesUpdate>("updateUniqueModulesProperties") {
    uniqueModulesProperties.set(layout.projectDirectory.file(
        "src/main/resources/de/jjohannes/gradle/moduledependencies/unique_modules.properties"))
}

tasks.processResources {
    dependsOn(updateUniqueModulesProperties)
}