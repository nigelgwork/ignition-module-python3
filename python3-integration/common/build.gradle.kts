plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // Ignition SDK dependencies (provided by platform)
    compileOnly(libs.ignition.common)
}
