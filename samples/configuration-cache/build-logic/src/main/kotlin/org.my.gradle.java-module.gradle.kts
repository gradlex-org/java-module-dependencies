plugins {
    id("de.jjohannes.java-module-dependencies")
}

group = "org.my"

tasks.test {
    useJUnitPlatform()
}

dependencies {
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
