plugins {
    base
    id("io.ia.sdk.modl") version "0.1.1"
    id("org.owasp.dependencycheck") version "9.0.9"
    checkstyle
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
    // IMPORTANT: Module ID must remain consistent for upgrade compatibility
    // Changed from com.gaskony.python3integration back to original to allow upgrades
    id.set("com.inductiveautomation.ignition.examples.python3")
    moduleVersion.set(project.version.toString())

    // Include vendor name in description
    moduleDescription.set("Enables Python 3 scripting functions in Ignition via subprocess process pool. Developed by Gaskony.")

    requiredIgnitionVersion.set("8.3.0")
    requiredFrameworkVersion.set("8")

    // Free module - no license required
    freeModule.set(true)

    projectScopes.putAll(
        mapOf(
            ":common" to "G",
            ":gateway" to "G"
            // Designer scope temporarily disabled until RPC API is verified
            // ":designer" to "D"
        )
    )

    moduleDependencies.putAll(
        mapOf()
    )

    hooks.putAll(
        mapOf(
            "com.inductiveautomation.ignition.examples.python3.gateway.GatewayHook" to "G"
            // Designer hook temporarily disabled
            // "com.inductiveautomation.ignition.examples.python3.designer.DesignerHook" to "D"
        )
    )

    // Enable module signing with self-signed certificate
    // Signing configured via sign.props file
    skipModlSigning.set(false)
}

// OWASP Dependency Check Configuration
dependencyCheck {
    format = org.owasp.dependencycheck.reporting.ReportGenerator.Format.HTML.toString()
    outputDirectory = "build/reports"

    // Suppress false positives and known issues
    suppressionFile = "config/owasp-suppressions.xml"

    // Fail build on CVSS score >= 7 (High or Critical)
    failBuildOnCVSS = 7.0f

    // Check all configurations
    scanConfigurations = listOf("runtimeClasspath", "compileClasspath")
}

// Checkstyle Configuration
checkstyle {
    toolVersion = "10.12.5"
    configFile = file("config/checkstyle/checkstyle.xml")
}

// Apply Checkstyle to all subprojects
subprojects {
    apply(plugin = "checkstyle")

    configure<CheckstyleExtension> {
        toolVersion = "10.12.5"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    }
}
