package de.jjohannes.gradle.moduledependencies.gradlebuild.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.Properties

abstract class UniqueModulesPropertiesUpdate : DefaultTask() {

    @TaskAction
    fun convert() {
        val modulesToRepoLocation = Properties()
        modulesToRepoLocation.load(getModulesPropertiesFromRepository().inputStream())
        val modulesToCoordinates = modulesToRepoLocation.toSortedMap { e1, e2 -> e1.toString().compareTo(e2.toString()) }.map { entry ->
            val split = entry.value.toString().split("/")
            val group = split.subList(4, split.size - 3).joinToString(".")
            val name = split[split.size - 3]
            "${entry.key}=$group:$name\n"
        }.joinToString("")
        project.layout.projectDirectory.file("src/main/resources/de/jjohannes/gradle/moduledependencies/unique_modules.properties").asFile.writeText(modulesToCoordinates.trim())
    }

    private fun getModulesPropertiesFromRepository(): File {
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
        val moduleProperties = detachedResolver.configurations.create("moduleProperties")
        val dep = detachedResolver.dependencies.add(moduleProperties.name, "com.github.sormuras.modules:modules:1")
        (dep as ExternalModuleDependency).isChanging = true
        return moduleProperties.singleFile
    }
}