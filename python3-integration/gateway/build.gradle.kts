plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // Common subproject dependency
    implementation(projects.common)

    // SDK dependencies (provided by Ignition)
    compileOnly(libs.ignition.common)
    compileOnly(libs.ignition.gateway.api)
    compileOnly("com.google.code.gson:gson:2.10.1")

    // Third-party libraries to bundle in module
    modlImplementation("org.apache.commons:commons-compress:1.24.0")
}
