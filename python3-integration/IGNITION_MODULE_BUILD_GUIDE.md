# Ignition 8.3 Module Build Guide - Complete Working Configuration

**Purpose**: This guide provides a complete, working configuration for building Ignition 8.3 SDK modules that:
- ✓ Successfully compile and build
- ✓ Self-sign without errors
- ✓ Display vendor name in Gateway
- ✓ Include proper version management
- ✓ Bundle dependencies correctly

**Tested With**: Ignition 8.3.0, Java 17, Gradle 8.5

---

## Quick Start Checklist

Use this checklist to verify your module configuration:

- [ ] Gradle plugin version: `io.ia.sdk.modl` version `0.1.1`
- [ ] Java toolchain: JDK 17
- [ ] Repository configuration includes Inductive Automation nexus
- [ ] `settings.gradle.kts` exists with proper repositories
- [ ] `gradle/libs.versions.toml` exists with SDK dependencies
- [ ] Module signing is enabled (not skipped)
- [ ] Vendor name in module description
- [ ] All hook classes exist at specified paths
- [ ] Project scopes match directory structure

---

## Complete Working File Structure

```
your-module/
├── build.gradle.kts              # Root build file
├── settings.gradle.kts           # Repository and project configuration
├── gradle/
│   └── libs.versions.toml        # Dependency version catalog
├── version.properties            # Version management (optional but recommended)
├── common/
│   └── build.gradle.kts          # Common scope build
│   └── src/main/java/...
├── gateway/
│   └── build.gradle.kts          # Gateway scope build
│   └── src/main/java/...
└── designer/
    └── build.gradle.kts          # Designer scope build
    └── src/main/java/...
```

---

## File 1: `settings.gradle.kts` (CRITICAL)

This file configures repositories and must be correct for the SDK plugin to work.

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://nexus.inductiveautomation.com/repository/public/")
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        mavenLocal()
        maven {
            url = uri("https://nexus.inductiveautomation.com/repository/public/")
        }
        maven {
            url = uri("https://nexus.inductiveautomation.com/repository/inductiveautomation-beta/")
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "your-module-name"

include(":gateway", ":common", ":designer")
```

**Key Points**:
- `pluginManagement` block is REQUIRED for SDK plugin to resolve
- Inductive Automation nexus repository MUST be included
- Both `public` and `beta` repositories should be added
- `TYPESAFE_PROJECT_ACCESSORS` enables type-safe project dependencies

---

## File 2: `gradle/libs.versions.toml`

This file defines all dependency versions in one place.

```toml
[versions]
ignition = "8.3.0"

[libraries]
# Ignition SDK dependencies
ignition-common = { module = "com.inductiveautomation.ignitionsdk:ignition-common", version.ref = "ignition" }
ignition-gateway-api = { module = "com.inductiveautomation.ignitionsdk:gateway-api", version.ref = "ignition" }
ignition-designer-api = { module = "com.inductiveautomation.ignitionsdk:designer-api", version.ref = "ignition" }

# Add your third-party dependencies here
# example-lib = { module = "com.example:library", version = "1.0.0" }
```

**Why This Matters**:
- Centralized version management
- Type-safe dependency references
- Easy to update all dependencies at once
- Prevents version conflicts

---

## File 3: Root `build.gradle.kts`

This is the main configuration file that controls module generation.

```kotlin
plugins {
    base
    id("io.ia.sdk.modl") version "0.1.1"
}

// Version management (optional but recommended)
val versionProps = java.util.Properties()
file("version.properties").inputStream().use { versionProps.load(it) }
val versionMajor = versionProps.getProperty("version.major")
val versionMinor = versionProps.getProperty("version.minor")
val versionPatch = versionProps.getProperty("version.patch")
val moduleVersion = "$versionMajor.$versionMinor.$versionPatch"

version = moduleVersion
group = "com.yourcompany"  // Change to your company/organization

allprojects {
    version = moduleVersion
    group = "com.yourcompany"
}

ignitionModule {
    // Module file name (will be: YourModule-1.0.0.modl)
    fileName.set("YourModule-${project.version}")

    // Display name in Gateway
    name.set("Your Module Name")

    // Unique module ID (reverse domain notation)
    id.set("com.yourcompany.yourmodule")

    // Version
    moduleVersion.set(project.version.toString())

    // Description - INCLUDE VENDOR NAME HERE
    moduleDescription.set("Your module description. Developed by Your Company Name.")

    // Minimum Ignition version required
    requiredIgnitionVersion.set("8.3.0")

    // Framework version (always 8 for Ignition 8.x)
    requiredFrameworkVersion.set("8")

    // Free or licensed module
    freeModule.set(true)

    // Map subprojects to scopes
    // G = Gateway, D = Designer, C = Client (Vision)
    projectScopes.putAll(mapOf(
        ":gateway" to "G",
        ":common" to "DG",    // Common code used by both Designer and Gateway
        ":designer" to "D"
    ))

    // Hook classes (must match actual class locations)
    hooks.putAll(mapOf(
        "com.yourcompany.yourmodule.gateway.GatewayHook" to "G",
        "com.yourcompany.yourmodule.designer.DesignerHook" to "D"
    ))

    // Module signing - CRITICAL FOR SUCCESS
    skipModlSigning.set(false)  // Must be false or omitted
}
```

**Critical Configuration Points**:

### 1. Plugin Version
```kotlin
id("io.ia.sdk.modl") version "0.1.1"
```
- **0.1.1** is the working version for Ignition 8.3
- Do NOT use older versions like 0.0.x

### 2. Vendor Name Display
```kotlin
moduleDescription.set("Your module description. Developed by Your Company Name.")
```
- Ignition displays the module description in Gateway
- Include "Developed by [Company]" in description
- This is how vendor name appears to users

### 3. Module Signing
```kotlin
skipModlSigning.set(false)  // or omit this line entirely
```
- Setting to `false` enables self-signing
- The plugin automatically generates and signs with test certificate
- Omitting the line also enables signing (false is default)
- Setting to `true` will cause unsigned module (requires manual signing)

### 4. Project Scopes
```kotlin
projectScopes.putAll(mapOf(
    ":gateway" to "G",
    ":common" to "DG",
    ":designer" to "D"
))
```
- Must match your actual subproject names (from `include()` in settings.gradle.kts)
- Scopes: G = Gateway, D = Designer, C = Client, combinations like "DG" for shared code

### 5. Hook Classes
```kotlin
hooks.putAll(mapOf(
    "com.yourcompany.yourmodule.gateway.GatewayHook" to "G",
    "com.yourcompany.yourmodule.designer.DesignerHook" to "D"
))
```
- **CRITICAL**: Paths must exactly match your actual class locations
- Common error: Hook class path doesn't match `group` setting
- If `group = "com.yourcompany"` and project is `yourmodule`, gateway hook should be at:
  `gateway/src/main/java/com/yourcompany/yourmodule/gateway/GatewayHook.java`

---

## File 4: `version.properties` (Recommended)

This file enables automatic version management.

```properties
# Module Version Configuration
# Format: MAJOR.MINOR.PATCH

version.major=1
version.minor=0
version.patch=0
```

**Benefits**:
- Single source of truth for version
- Version appears in filename: `YourModule-1.0.0.modl`
- Easy to increment with scripts
- Consistent across all subprojects

**Version Increment Script** (`increment-version.sh`):

```bash
#!/bin/bash

VERSION_FILE="version.properties"

# Read current version
MAJOR=$(grep "version.major" $VERSION_FILE | cut -d'=' -f2)
MINOR=$(grep "version.minor" $VERSION_FILE | cut -d'=' -f2)
PATCH=$(grep "version.patch" $VERSION_FILE | cut -d'=' -f2)

echo "Current version: $MAJOR.$MINOR.$PATCH"

# Increment based on argument
case $1 in
    major)
        MAJOR=$((MAJOR + 1))
        MINOR=0
        PATCH=0
        ;;
    minor)
        MINOR=$((MINOR + 1))
        PATCH=0
        ;;
    patch)
        PATCH=$((PATCH + 1))
        ;;
    *)
        echo "Usage: $0 {major|minor|patch}"
        exit 1
        ;;
esac

echo "New version: $MAJOR.$MINOR.$PATCH"

# Write new version
cat > $VERSION_FILE << EOF
# Module Version Configuration
# This file is automatically updated by the build system
# Format: MAJOR.MINOR.PATCH

version.major=$MAJOR
version.minor=$MINOR
version.patch=$PATCH
EOF

echo "Version updated in $VERSION_FILE"
```

**Usage**:
```bash
chmod +x increment-version.sh
./increment-version.sh patch  # 1.0.0 -> 1.0.1
./increment-version.sh minor  # 1.0.1 -> 1.1.0
./increment-version.sh major  # 1.1.0 -> 2.0.0
```

---

## File 5: Subproject `build.gradle.kts` Files

Each subproject (gateway, designer, common) needs its own build file.

### `gateway/build.gradle.kts`

```kotlin
plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // Reference to common subproject
    implementation(projects.common)

    // SDK dependencies (provided by Ignition)
    compileOnly(libs.ignition.common)
    compileOnly(libs.ignition.gateway.api)

    // Third-party libraries to include in module
    // modlImplementation("com.example:library:1.0.0")
}
```

### `designer/build.gradle.kts`

```kotlin
plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(projects.common)
    compileOnly(libs.ignition.common)
    compileOnly(libs.ignition.designer.api)
}
```

### `common/build.gradle.kts`

```kotlin
plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    compileOnly(libs.ignition.common)

    // Third-party dependencies for common code
    api("com.google.code.gson:gson:2.10.1")
}
```

**Dependency Configuration Types**:

| Configuration | Purpose | Example Use Case |
|---------------|---------|------------------|
| `compileOnly` | Provided by Ignition at runtime | SDK APIs, SLF4J |
| `implementation` | Internal to your module | Subproject dependencies |
| `api` | Exposed to dependent subprojects | Common utilities |
| `modlImplementation` | Bundled in .modl file | Third-party libraries |

---

## Minimal Hook Classes

Your hook classes must exist at the paths specified in `build.gradle.kts`.

### `GatewayHook.java`

```java
package com.yourcompany.yourmodule.gateway;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GatewayHook extends AbstractGatewayModuleHook {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private GatewayContext context;

    @Override
    public void setup(GatewayContext context) {
        this.context = context;
        logger.info("Your Module Gateway setup starting");
    }

    @Override
    public void startup(LicenseState licenseState) {
        logger.info("Your Module Gateway started successfully");
    }

    @Override
    public void shutdown() {
        logger.info("Your Module Gateway shutting down");
    }

    @Override
    public boolean isMakerEditionCompatible() {
        return true;
    }

    @Override
    public boolean isFreeModule() {
        return true;  // Set to false if using licensing
    }
}
```

### `DesignerHook.java`

```java
package com.yourcompany.yourmodule.designer;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.designer.model.AbstractDesignerModuleHook;
import com.inductiveautomation.ignition.designer.model.DesignerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DesignerHook extends AbstractDesignerModuleHook {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private DesignerContext context;

    @Override
    public void startup(DesignerContext context, LicenseState activationState) {
        this.context = context;
        logger.info("Your Module Designer started");
    }

    @Override
    public void shutdown() {
        logger.info("Your Module Designer shutting down");
    }
}
```

---

## Build Commands

### Clean Build
```bash
./gradlew clean build
```

### Build Without Tests
```bash
./gradlew clean build -x test
```

### View Build Output
```bash
ls -lh build/*.modl
```

### View Module Contents
```bash
# View module.xml
unzip -p build/YourModule-1.0.0.modl module.xml

# List all files
unzip -l build/YourModule-1.0.0.modl
```

---

## Verification Steps

After building, verify your module is correct:

### 1. Check Module File Exists
```bash
ls -lh build/*.modl
# Should show: YourModule-1.0.0.modl (with version in filename)
```

### 2. Verify Module.xml Contents
```bash
unzip -p build/YourModule-*.modl module.xml
```

**Check for**:
- `<name>` matches what you set
- `<id>` is unique and correct
- `<version>` matches your version.properties
- `<description>` includes vendor name
- `<hook>` entries match your hook classes
- `<jar>` entries include all dependencies

### 3. Verify Signing
```bash
# Check for signing
unzip -l build/YourModule-*.modl | grep -i sign

# Should show files like:
# META-INF/CERT.RSA
# META-INF/CERT.SF
# META-INF/MANIFEST.MF
```

If these files exist, module is signed.

### 4. Check Build Output
```bash
./gradlew build --info | grep -i sign
```

Look for:
```
> Task :signModule
```

If you see `SKIPPED`, signing was disabled.

---

## Common Issues and Solutions

### Issue 1: "Cannot find symbol: class Mounting"

**Cause**: Trying to use internal Ignition APIs not exposed in SDK

**Solution**: Use only public SDK APIs. For web endpoints, research proper approach or use alternative architecture.

### Issue 2: "Plugin io.ia.sdk.modl not found"

**Cause**: Repository configuration missing or incorrect

**Solution**: Ensure `settings.gradle.kts` includes:
```kotlin
maven {
    url = uri("https://nexus.inductiveautomation.com/repository/public/")
}
```

### Issue 3: Module won't load - "Hook class not found"

**Cause**: Hook class path doesn't match specification

**Solution**:
1. Check `group` in build.gradle.kts
2. Verify package name in Java file matches
3. Ensure path: `group/module/scope/HookName.java`

Example:
- Group: `com.yourcompany`
- Module ID: `com.yourcompany.yourmodule`
- Gateway Hook must be: `gateway/src/main/java/com/yourcompany/yourmodule/gateway/GatewayHook.java`

### Issue 4: Dependencies not bundled

**Cause**: Using `compileOnly` instead of `modlImplementation`

**Solution**: Use `modlImplementation` for libraries to bundle:
```kotlin
dependencies {
    compileOnly(libs.ignition.gateway.api)  // SDK - don't bundle
    modlImplementation("com.google.code.gson:gson:2.10.1")  // Bundle this
}
```

### Issue 5: Unsigned module

**Symptom**: Gateway rejects module saying it's not signed

**Solution**: Check `skipModlSigning` is not set to `true`:
```kotlin
ignitionModule {
    skipModlSigning.set(false)  // or omit this line
}
```

### Issue 6: Wrong Java version

**Error**: "Unsupported class file major version"

**Solution**: Ignition 8.3 requires Java 17. Set in all subproject build files:
```kotlin
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
```

Verify system Java:
```bash
java -version  # Should show version 17
```

### Issue 7: Version not appearing in filename

**Cause**: Not using `fileName.set()` with version

**Solution**:
```kotlin
ignitionModule {
    fileName.set("YourModule-${project.version}")
}
```

### Issue 8: Vendor name not showing

**Cause**: Not included in module description

**Solution**: Add to description:
```kotlin
moduleDescription.set("Your module description. Developed by Your Company.")
```

---

## Complete Minimal Working Example

Here's a complete minimal module that will build successfully:

**Directory structure**:
```
my-module/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── version.properties
├── gateway/
│   ├── build.gradle.kts
│   └── src/main/java/com/example/mymodule/gateway/
│       └── GatewayHook.java
└── designer/
    ├── build.gradle.kts
    └── src/main/java/com/example/mymodule/designer/
        └── DesignerHook.java
```

**`settings.gradle.kts`**:
```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = uri("https://nexus.inductiveautomation.com/repository/public/") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        maven { url = uri("https://nexus.inductiveautomation.com/repository/public/") }
    }
}

rootProject.name = "my-module"
include(":gateway", ":designer")
```

**`build.gradle.kts`**:
```kotlin
plugins {
    base
    id("io.ia.sdk.modl") version "0.1.1"
}

version = "1.0.0"
group = "com.example"

ignitionModule {
    fileName.set("MyModule-${project.version}")
    name.set("My Test Module")
    id.set("com.example.mymodule")
    moduleVersion.set(project.version.toString())
    moduleDescription.set("A test module. Developed by Example Corp.")
    requiredIgnitionVersion.set("8.3.0")
    freeModule.set(true)

    projectScopes.putAll(mapOf(
        ":gateway" to "G",
        ":designer" to "D"
    ))

    hooks.putAll(mapOf(
        "com.example.mymodule.gateway.GatewayHook" to "G",
        "com.example.mymodule.designer.DesignerHook" to "D"
    ))

    skipModlSigning.set(false)
}
```

**`gradle/libs.versions.toml`**:
```toml
[versions]
ignition = "8.3.0"

[libraries]
ignition-common = { module = "com.inductiveautomation.ignitionsdk:ignition-common", version.ref = "ignition" }
ignition-gateway-api = { module = "com.inductiveautomation.ignitionsdk:gateway-api", version.ref = "ignition" }
ignition-designer-api = { module = "com.inductiveautomation.ignitionsdk:designer-api", version.ref = "ignition" }
```

**`gateway/build.gradle.kts`**:
```kotlin
plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    compileOnly(libs.ignition.common)
    compileOnly(libs.ignition.gateway.api)
}
```

**`designer/build.gradle.kts`**:
```kotlin
plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    compileOnly(libs.ignition.common)
    compileOnly(libs.ignition.designer.api)
}
```

**Build and verify**:
```bash
./gradlew clean build
ls -lh build/*.modl
unzip -p build/MyModule-1.0.0.modl module.xml
```

---

## Testing Installation

### Install Module
1. Open Gateway web UI: `http://localhost:8088`
2. Navigate to: **Config → System → Modules**
3. Click **"Install or Upgrade a Module"**
4. Select your `.modl` file
5. Click **Install**

### Verify Installation
1. Module appears in modules list
2. Name shows correctly: "My Test Module"
3. Description shows vendor: "Developed by Example Corp."
4. Version matches: "1.0.0"
5. Status shows: "Running" or "Loaded"

### Check Logs
```bash
# Gateway logs
tail -f /path/to/ignition/logs/wrapper.log | grep "My Test Module"

# Docker
docker logs container-name | grep "My Test Module"
```

Look for:
```
INFO  [c.e.m.g.GatewayHook] My Test Module Gateway setup starting
INFO  [c.e.m.g.GatewayHook] My Test Module Gateway started successfully
```

---

## Key Success Factors

### ✓ Must Have:
1. **SDK Plugin 0.1.1**: Use `id("io.ia.sdk.modl") version "0.1.1"`
2. **Repository Config**: Include IA nexus in `settings.gradle.kts`
3. **Java 17**: Set toolchain in all subprojects
4. **Hook Classes**: Must exist at exact paths specified
5. **Module Signing**: Enabled (not skipped)
6. **Vendor in Description**: Include "Developed by [Name]"

### ✓ Best Practices:
1. Use `version.properties` for version management
2. Use `gradle/libs.versions.toml` for dependencies
3. Include version in filename: `fileName.set("Module-${version}")`
4. Test build with `./gradlew clean build`
5. Verify module.xml contents before installing
6. Check Gateway logs after installation

---

## Troubleshooting Checklist

If build fails, check in this order:

- [ ] Java 17 installed: `java -version`
- [ ] Gradle wrapper present: `./gradlew --version`
- [ ] `settings.gradle.kts` includes IA repository
- [ ] Plugin version is `0.1.1`
- [ ] `gradle/libs.versions.toml` exists with SDK dependencies
- [ ] Hook classes exist at specified paths
- [ ] Package names match group + module ID
- [ ] `skipModlSigning` is not set to `true`
- [ ] All subproject `build.gradle.kts` files set Java 17 toolchain

If module won't load in Gateway:

- [ ] Module file is signed (check for META-INF/CERT.* files)
- [ ] Hook class paths match module.xml
- [ ] Module ID is unique (no conflicts)
- [ ] Ignition version is 8.3.0 or higher
- [ ] Unsigned modules are allowed in Gateway (if using dev mode)
- [ ] Check wrapper.log for specific error messages

---

## Template Repository Structure

For easy reuse, create a template repository with this structure:

```
ignition-module-template/
├── .gitignore
├── README.md
├── build.gradle.kts              # Template with TODOs
├── settings.gradle.kts
├── gradle/
│   ├── wrapper/
│   │   ├── gradle-wrapper.jar
│   │   └── gradle-wrapper.properties
│   └── libs.versions.toml
├── gradlew
├── gradlew.bat
├── version.properties
├── increment-version.sh
├── gateway/
│   ├── build.gradle.kts
│   └── src/main/java/com/company/module/gateway/
│       └── GatewayHook.java      # Minimal template
├── designer/
│   ├── build.gradle.kts
│   └── src/main/java/com/company/module/designer/
│       └── DesignerHook.java     # Minimal template
└── common/
    ├── build.gradle.kts
    └── src/main/java/com/company/module/common/
        └── Constants.java         # Minimal template
```

**`.gitignore`**:
```
.gradle/
build/
.idea/
*.iml
*.ipr
*.iws
out/
.DS_Store
```

---

## Resources

### Official Documentation
- Ignition SDK Docs: https://www.sdk-docs.inductiveautomation.com/
- SDK Examples: https://github.com/inductiveautomation/ignition-sdk-examples
- Forum: https://forum.inductiveautomation.com/c/module-development/7

### This Working Example
- Location: `/modules/claude-code-module/`
- Built Module: `/modules/claude-code-module/build/ClaudeCodeIntegration-1.3.1.modl`
- Successfully tested with Ignition 8.3.0 in Docker

### Key Files From This Project
- `build.gradle.kts`: Root build configuration (lines 1-46)
- `settings.gradle.kts`: Repository setup (lines 1-31)
- `gradle/libs.versions.toml`: Dependency catalog (lines 1-15)
- `gateway/build.gradle.kts`: Gateway scope config (lines 1-16)

---

## Summary - Copy This Configuration

**The working formula**:

1. **Plugin**: `id("io.ia.sdk.modl") version "0.1.1"`
2. **Repository**: IA nexus in `settings.gradle.kts`
3. **Java**: 17 via toolchain in all subprojects
4. **Signing**: Enabled (don't skip)
5. **Vendor**: In module description
6. **Version**: In filename via `fileName.set()`
7. **Hooks**: Exact path matching

Follow this guide exactly, and your Ignition 8.3 module will build successfully with proper signing and vendor display.

---

**Last Updated**: October 13, 2025
**Tested With**: Ignition 8.3.0, Gradle 8.5, Java 17
**Module Example**: Claude Code Integration v1.3.1
**Status**: ✓ VERIFIED WORKING
