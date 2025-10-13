plugins {
    `java-library`
    id("io.ia.sdk.modl") version "0.1.1"
}

allprojects {
    group = "com.inductiveautomation.ignition.examples"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven {
            url = uri("https://nexus.inductiveautomation.com/repository/inductiveautomation-releases")
        }
        maven {
            url = uri("https://nexus.inductiveautomation.com/repository/inductiveautomation-snapshots")
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

ignitionModule {
    fileName.set("Python3Integration")
    name.set("Python 3 Integration")
    id.set("com.inductiveautomation.ignition.examples.python3")
    moduleVersion.set("${project.version}")
    moduleDescription.set("Enables Python 3 scripting functions in Ignition via subprocess process pool")
    requiredIgnitionVersion.set("8.3.0")
    freeModule.set(true)

    projectScopes.putAll(
        mapOf(
            ":gateway" to "G",
            ":common" to "GC"
        )
    )

    moduleDependencies.putAll(
        mapOf()
    )

    hooks.putAll(
        mapOf(
            "com.inductiveautomation.ignition.examples.python3.gateway.GatewayHook" to "G"
        )
    )

    applyInductiveArtifactRepo.set(true)
    skipModlSigning.set(true)
}
