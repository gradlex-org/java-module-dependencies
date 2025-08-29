plugins {
    id("org.my.gradle.platform")
}


dependencies {
    api(platform("com.fasterxml.jackson:jackson-bom:2.19.1"))
    api(platform("org.junit:junit-bom:5.13.4"))
}

dependencies.constraints {
    javaModuleDependencies {
        api(gav("org.apache.xmlbeans", "5.0.1"))
        api(gav("org.slf4j", "1.7.28"))
        api(gav("org.slf4j.simple", "1.7.28"))
    }
}