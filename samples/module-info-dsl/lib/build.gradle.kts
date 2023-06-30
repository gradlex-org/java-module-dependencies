plugins {
    id("org.example.java-module")
}

testModuleInfo {
    requires("org.junit.jupiter.api")
}

// println(configurations.compileClasspath.get().files)