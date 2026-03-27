plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("com.autonomousapps:dependency-analysis-gradle-plugin:3.6.1")
    implementation("org.gradlex:java-module-dependencies:1.12.1")
    implementation("org.gradlex:java-module-testing:1.8.1")
    implementation("org.gradlex:jvm-dependency-conflict-resolution:2.5")
}