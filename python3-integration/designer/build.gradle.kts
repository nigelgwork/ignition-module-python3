plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // Common scope dependency
    api(projects.common)

    // Ignition SDK dependencies (provided by platform)
    compileOnly(libs.ignition.common)
    compileOnly(libs.ignition.designer.api)

    // Logging
    compileOnly("org.slf4j:slf4j-api:1.7.36")
}
