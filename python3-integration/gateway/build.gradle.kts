plugins {
    `java-library`
}

dependencies {
    compileOnly("com.inductiveautomation.ignitionsdk:gateway-api:8.3.0")
    compileOnly("com.google.code.gson:gson:2.10.1")

    // For tar.gz extraction
    modlImplementation("org.apache.commons:commons-compress:1.24.0")

    api(project(":common"))
}
