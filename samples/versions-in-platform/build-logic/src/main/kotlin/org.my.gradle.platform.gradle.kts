plugins {
    id("java-platform")
    id("de.jjohannes.java-module-dependencies")
}

group = "org.my"

javaPlatform.allowDependencies() // Use existing Platforms/BOMs
