package gradlexbuild

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.util.Properties
import javax.inject.Inject

abstract class UniqueModulesPropertiesUpdate : DefaultTask() {

    @get:Inject
    abstract val layout: ProjectLayout

    @get:InputFiles
    abstract val modulesProperties: ConfigurableFileCollection

    @get:OutputFile
    abstract val uniqueModulesProperties: RegularFileProperty

    @TaskAction
    fun convert() {
        val modulesToRepoLocation = Properties()
        modulesToRepoLocation.load(modulesProperties.singleFile.inputStream())
        val modulesToCoordinates = modulesToRepoLocation.toSortedMap { e1, e2 -> e1.toString().compareTo(e2.toString()) }.map { entry ->
            val split = entry.value.toString().split("/")
            val group = split.subList(4, split.size - 3).joinToString(".")
            val name = split[split.size - 3]
            "${entry.key}=$group:$name\n"
        }.joinToString("")
        uniqueModulesProperties.get().asFile.writeText(modulesToCoordinates.trim())
    }
}