plugins {
    id("de.jjohannes.java-module-dependencies")
}

group = "org.my.group"

// By default, the group would be used, we override that:
javaModuleDependencies.ownModuleNamesPrefix.set("org.my")
