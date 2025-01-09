import gradlexbuild.UniqueModulesPropertiesUpdate
import org.gradle.api.internal.project.ProjectInternal

plugins {
    id("java")
}

val detachedResolver: ProjectInternal.DetachedResolver = (project as ProjectInternal).newDetachedResolver()
detachedResolver.repositories.ivy {
    name = "Modules Properties Repository"
    url = project.uri("https://raw.githubusercontent.com/sormuras/modules/main/com.github.sormuras.modules")
    metadataSources.artifact()
    patternLayout {
        artifact("[organisation]/[module].properties")
        ivy("[module]/[revision]/ivy.xml")
        setM2compatible(true)
    }
}
val modulePropertiesScope = detachedResolver.configurations.dependencyScope("moduleProperties")
val modulePropertiesPath = detachedResolver.configurations.resolvable("modulePropertiesPath") {
    extendsFrom(modulePropertiesScope.get())
}
val dep = detachedResolver.dependencies.add(modulePropertiesScope.name, "com.github.sormuras.modules:modules:1")
(dep as ExternalModuleDependency).isChanging = true

val updateUniqueModulesProperties = tasks.register<UniqueModulesPropertiesUpdate>("updateUniqueModulesProperties") {
    skipUpdate.set(providers.environmentVariable("CI").getOrElse("false").toBoolean())
    modulesProperties.from(modulePropertiesPath)
    uniqueModulesProperties.set(layout.projectDirectory.file(
        "src/main/resources/org/gradlex/javamodule/dependencies/unique_modules.properties")
    )
}

sourceSets.main {
    resources.setSrcDirs(listOf(updateUniqueModulesProperties.map {
        it.uniqueModulesProperties.get().asFile.parentFile.parentFile.parentFile.parentFile.parentFile
    }))
}

