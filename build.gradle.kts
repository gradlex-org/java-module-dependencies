version = "1.12.1"

configurations.compileClasspath {
    // Allow Java 11 dependencies on compile classpath
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 11)
}

dependencies {
    implementation("org.ow2.asm:asm:9.10.1")
    implementation("com.github.javaparser:javaparser-core:3.28.2")
    compileOnly("org.gradlex:extra-java-module-info:1.14")
    compileOnly("com.autonomousapps:dependency-analysis-gradle-plugin:3.15.0")
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
