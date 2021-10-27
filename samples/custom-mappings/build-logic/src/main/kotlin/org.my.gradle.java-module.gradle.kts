plugins {
    id("de.jjohannes.java-module-dependencies")
}

group = "org.my"

javaModuleDependencies {
    moduleNameToGA.put("org.apache.commons.lang3", "org.apache.commons:commons-lang3")
}
