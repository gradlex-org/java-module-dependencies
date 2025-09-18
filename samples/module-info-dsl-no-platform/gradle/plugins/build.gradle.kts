plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("com.autonomousapps:dependency-analysis-gradle-plugin:3.0.4")
    implementation("org.gradlex:java-module-dependencies:1.10")
    implementation("org.gradlex:java-module-testing:1.7")
    implementation("org.gradlex:jvm-dependency-conflict-resolution:2.4")
}