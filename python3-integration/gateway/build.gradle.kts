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

    // SDK dependencies (provided by Ignition)
    compileOnly(libs.ignition.common)
    compileOnly(libs.ignition.gateway.api)
    compileOnly(libs.ignition.perspective.gateway)
    compileOnly(libs.ignition.perspective.common)
    compileOnly("com.google.code.gson:gson:2.10.1")
    compileOnly("javax.servlet:javax.servlet-api:4.0.1")

    // Third-party libraries to bundle in module
    modlImplementation("org.apache.commons:commons-compress:1.24.0")
}
