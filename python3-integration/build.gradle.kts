plugins {
    base
    id("io.ia.sdk.modl") version "0.1.1"
}

// Load version from version.properties
val versionProps = java.util.Properties()
file("version.properties").inputStream().use { versionProps.load(it) }
val versionMajor = versionProps.getProperty("version.major")
val versionMinor = versionProps.getProperty("version.minor")
val versionPatch = versionProps.getProperty("version.patch")
val moduleVersion = "$versionMajor.$versionMinor.$versionPatch"

version = moduleVersion
group = "com.gaskony"

allprojects {
    version = moduleVersion
    group = "com.gaskony"
}

ignitionModule {
    // Include version in filename for version control
    fileName.set("Python3Integration-${project.version}")

    name.set("Python 3 Integration")
    id.set("com.gaskony.python3integration")
    moduleVersion.set(project.version.toString())

    // Include vendor name in description
    moduleDescription.set("Enables Python 3 scripting functions in Ignition via subprocess process pool. Developed by Gaskony.")

    requiredIgnitionVersion.set("8.3.0")
    requiredFrameworkVersion.set("8")
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

    // Skip signing for development/testing
    // Enable signing (false) when you have proper certificates
    skipModlSigning.set(true)
}
