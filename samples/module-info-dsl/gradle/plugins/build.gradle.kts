plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("com.autonomousapps:dependency-analysis-gradle-plugin:3.4.0")
    implementation("org.gradlex:java-module-dependencies:1.11")
    implementation("org.gradlex:java-module-testing:1.8")
    implementation("org.gradlex:jvm-dependency-conflict-resolution:2.4")
}