rootProject.name = "python3-integration"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://nexus.inductiveautomation.com/repository/inductiveautomation-releases")
        }
    }
}

include(":common")
include(":gateway")
