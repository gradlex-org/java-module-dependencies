plugins {
    id("org.example.java-module-app")
}

application {
    applicationDefaultJvmArgs = listOf("-ea")
    mainClass.set("org.example.app.App")
    mainModule.set("org.example.app")
}

mainModuleInfo {
    runtimeOnly("org.slf4j.simple")
}

testModuleInfo {
    requires("org.junit.jupiter.api")
}
