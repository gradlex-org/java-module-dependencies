package gradlexbuild

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.util.internal.VersionNumber
import java.util.Properties
import javax.inject.Inject

abstract class UniqueModulesPropertiesUpdate : DefaultTask() {

    @get:Inject
    abstract val layout: ProjectLayout

    @get:Input
    abstract val skipUpdate: Property<Boolean>

    @get:InputFiles
    abstract val modulesProperties: ConfigurableFileCollection

    @get:OutputFile
    abstract val uniqueModulesProperties: RegularFileProperty

    @TaskAction
    fun convert() {
        if (skipUpdate.get()) {
            return
        }

        val modulesToRepoLocation = Properties()
        modulesToRepoLocation.load(modulesProperties.singleFile.inputStream())
        val modules = modulesToRepoLocation.toSortedMap { e1, e2 -> e1.toString().compareTo(e2.toString()) }.map { entry ->
            val split = entry.value.toString().split("/")
            val group = split.subList(4, split.size - 3).joinToString(".")
            val name = split[split.size - 3]
                // Special handling of "wrong" entries
                .replace("-debug-jdk18on", "-jdk18on")
            val version = split[split.size - 2]
            Module(entry.key.toString(), "$group:$name", version)
        }.groupBy { it.ga }.values.map { moduleList ->
            moduleList.maxBy { VersionNumber.parse(it.version) }
        }.sortedBy { it.name }

        val modulesToCoordinates = modules.map { "${it.name}=${it.ga}\n" }
        uniqueModulesProperties.get().asFile.writeText(modulesToCoordinates.joinToString("").trim())
    }

    data class Module(val name: String, val ga: String, val version: String)
}