import java.util.Properties
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.util.internal.VersionNumber

version = "1.11"

configurations.compileClasspath {
    // Allow Java 11 dependencies on compile classpath
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 11)
}

dependencies {
    implementation("org.ow2.asm:asm:9.9")
    compileOnly("org.gradlex:extra-java-module-info:1.13.1")
    compileOnly("com.autonomousapps:dependency-analysis-gradle-plugin:3.5.1")
}

jvmDependencyConflicts.patch {
    module("com.autonomousapps:dependency-analysis-gradle-plugin") {
        removeDependency("dev.zacsweers.moshix:moshi-sealed-runtime")
        removeDependency("javax.inject:javax.inject")
    }
}

publishingConventions {
    pluginPortal("${project.group}.${project.name}") {
        implementationClass("org.gradlex.javamodule.dependencies.JavaModuleDependenciesPlugin")
        displayName("Java Module Dependencies Gradle Plugin")
        description("A plugin that makes Gradle respect the dependencies defined in 'module-info.java' files.")
        tags("gradlex", "java", "modularity", "jigsaw", "jpms", "dependencies", "versions")
    }
    pluginPortal("${project.group}.java-module-versions") {
        implementationClass("org.gradlex.javamodule.dependencies.JavaModuleVersionsPlugin")
        displayName("Java Module Versions Gradle Plugin")
        description("A plugin that makes Gradle respect the dependencies defined in 'module-info.java' files.")
    }
    gitHub("https://github.com/gradlex-org/java-module-dependencies")
    developer {
        id = "jjohannes"
        name = "Jendrik Johannes"
        email = "jendrik@gradlex.org"
    }
}

testingConventions { testGradleVersions("7.4", "7.6.5", "8.0.2", "8.14.3") }

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
val modulePropertiesPath =
    detachedResolver.configurations.resolvable("modulePropertiesPath") { extendsFrom(modulePropertiesScope.get()) }
val dep =
    detachedResolver.dependencies.add(modulePropertiesScope.name, "com.github.sormuras.modules:modules:1@properties")

(dep as ExternalModuleDependency).isChanging = true

val updateUniqueModulesProperties =
    tasks.register<UniqueModulesPropertiesUpdate>("updateUniqueModulesProperties") {
        skipUpdate = providers.environmentVariable("CI").getOrElse("false").toBoolean()
        modulesProperties.from(modulePropertiesPath)
        uniqueModulesProperties =
            layout.projectDirectory.file(
                "src/main/resources/org/gradlex/javamodule/dependencies/unique_modules.properties"
            )
    }

sourceSets.main {
    resources.setSrcDirs(
        listOf(
            updateUniqueModulesProperties.map {
                it.uniqueModulesProperties.get().asFile.parentFile.parentFile.parentFile.parentFile.parentFile
            }
        )
    )
}

abstract class UniqueModulesPropertiesUpdate : DefaultTask() {

    @get:Inject abstract val layout: ProjectLayout

    @get:Input abstract val skipUpdate: Property<Boolean>

    @get:InputFiles abstract val modulesProperties: ConfigurableFileCollection

    @get:OutputFile abstract val uniqueModulesProperties: RegularFileProperty

    @TaskAction
    fun convert() {
        if (skipUpdate.get()) {
            return
        }

        val modulesToRepoLocation = Properties()
        modulesToRepoLocation.load(modulesProperties.singleFile.inputStream())
        val modules =
            modulesToRepoLocation
                .toSortedMap { e1, e2 -> e1.toString().compareTo(e2.toString()) }
                .map { entry ->
                    val split = entry.value.toString().split("/")
                    val group = split.subList(4, split.size - 3).joinToString(".")
                    val name =
                        split[split.size - 3]
                            // Special handling of "wrong" entries
                            .replace("-debug-jdk18on", "-jdk18on")
                    val version = split[split.size - 2]
                    Module(entry.key.toString(), "$group:$name", version)
                }
                .groupBy { it.ga }
                .values
                .map { moduleList -> moduleList.maxBy { VersionNumber.parse(it.version) } }
                .sortedBy { it.name }

        val modulesToCoordinates = modules.map { "${it.name}=${it.ga}\n" }
        uniqueModulesProperties.get().asFile.writeText(modulesToCoordinates.joinToString("").trim())
    }

    data class Module(val name: String, val ga: String, val version: String)
}
