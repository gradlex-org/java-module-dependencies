plugins {
    id("java-platform")
    id("org.gradlex.java-module-dependencies")
}

group = "org.my"

javaPlatform.allowDependencies() // Use existing Platforms/BOMs
