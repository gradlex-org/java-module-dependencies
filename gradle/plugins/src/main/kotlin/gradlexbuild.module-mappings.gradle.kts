import gradlexbuild.UniqueModulesPropertiesUpdate

plugins {
    id("base")
}

val updateUniqueModulesProperties = tasks.register<UniqueModulesPropertiesUpdate>("updateUniqueModulesProperties")

tasks.assemble {
    dependsOn(updateUniqueModulesProperties)
}
