plugins {
    id("org.my.gradle.java-module")
    id("application")
}

application {
    mainClass.set("org.my.app.App")
    mainModule.set("org.my.app")
}
