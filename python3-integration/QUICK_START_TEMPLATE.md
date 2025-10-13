# Ignition 8.3 Module - Quick Start Template

**Purpose**: Copy these files exactly to create a working Ignition 8.3 module that builds and signs successfully.

---

## Step 1: Create Directory Structure

```bash
mkdir -p my-module/{gateway,designer,common}/src/main/java/com/company/module/{gateway,designer,common}
mkdir -p my-module/gradle
cd my-module
```

---

## Step 2: Create `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://nexus.inductiveautomation.com/repository/public/")
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        maven {
            url = uri("https://nexus.inductiveautomation.com/repository/public/")
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "my-module"
include(":gateway", ":designer", ":common")
```

---

## Step 3: Create `build.gradle.kts`

```kotlin
plugins {
    base
    id("io.ia.sdk.modl") version "0.1.1"
}

version = "1.0.0"
group = "com.company"

allprojects {
    version = "1.0.0"
    group = "com.company"
}

ignitionModule {
    fileName.set("MyModule-${project.version}")
    name.set("My Module")
    id.set("com.company.mymodule")
    moduleVersion.set(project.version.toString())
    moduleDescription.set("My module description. Developed by My Company.")
    requiredIgnitionVersion.set("8.3.0")
    requiredFrameworkVersion.set("8")
    freeModule.set(true)

    projectScopes.putAll(mapOf(
        ":gateway" to "G",
        ":common" to "DG",
        ":designer" to "D"
    ))

    hooks.putAll(mapOf(
        "com.company.module.gateway.GatewayHook" to "G",
        "com.company.module.designer.DesignerHook" to "D"
    ))

    skipModlSigning.set(false)
}
```

---

## Step 4: Create `gradle/libs.versions.toml`

```toml
[versions]
ignition = "8.3.0"

[libraries]
ignition-common = { module = "com.inductiveautomation.ignitionsdk:ignition-common", version.ref = "ignition" }
ignition-gateway-api = { module = "com.inductiveautomation.ignitionsdk:gateway-api", version.ref = "ignition" }
ignition-designer-api = { module = "com.inductiveautomation.ignitionsdk:designer-api", version.ref = "ignition" }
```

---

## Step 5: Create `gateway/build.gradle.kts`

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
    compileOnly(libs.ignition.gateway.api)
}
```

---

## Step 6: Create `designer/build.gradle.kts`

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

---

## Step 7: Create `common/build.gradle.kts`

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
}
```

---

## Step 8: Create `gateway/src/main/java/com/company/module/gateway/GatewayHook.java`

```java
package com.company.module.gateway;

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
        logger.info("My Module Gateway setup starting");
    }

    @Override
    public void startup(LicenseState licenseState) {
        logger.info("My Module Gateway started successfully");
    }

    @Override
    public void shutdown() {
        logger.info("My Module Gateway shutting down");
    }

    @Override
    public boolean isMakerEditionCompatible() {
        return true;
    }

    @Override
    public boolean isFreeModule() {
        return true;
    }
}
```

---

## Step 9: Create `designer/src/main/java/com/company/module/designer/DesignerHook.java`

```java
package com.company.module.designer;

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
        logger.info("My Module Designer started");
    }

    @Override
    public void shutdown() {
        logger.info("My Module Designer shutting down");
    }
}
```

---

## Step 10: Initialize Gradle Wrapper

```bash
gradle wrapper --gradle-version 8.5
```

---

## Step 11: Build

```bash
./gradlew clean build
```

**Expected output**:
```
BUILD SUCCESSFUL in 10s
```

**Check result**:
```bash
ls -lh build/*.modl
```

Should show: `MyModule-1.0.0.modl`

---

## Customization Checklist

Before building, replace these values:

### In all files:
- [ ] `com.company` → your reverse domain
- [ ] `module` → your module ID component
- [ ] `mymodule` → your module ID

### In `build.gradle.kts`:
- [ ] `group = "com.company"` → your domain
- [ ] `name.set("My Module")` → your display name
- [ ] `id.set("com.company.mymodule")` → unique ID
- [ ] `moduleDescription.set("...")` → your description + vendor name
- [ ] `fileName.set("MyModule-...")` → your filename prefix

### In `settings.gradle.kts`:
- [ ] `rootProject.name = "my-module"` → your project name

### Directory structure:
- [ ] Rename packages to match your `group` setting
- [ ] Update hook class packages
- [ ] Verify paths in `hooks.putAll()`

---

## Verification Commands

```bash
# Build
./gradlew clean build

# Check module file
ls -lh build/*.modl

# View module.xml
unzip -p build/*.modl module.xml

# Check if signed
unzip -l build/*.modl | grep META-INF
```

**Must see**:
- `META-INF/CERT.RSA`
- `META-INF/CERT.SF`
- `META-INF/MANIFEST.MF`

---

## Common Errors and Fixes

### "Plugin io.ia.sdk.modl not found"
**Fix**: Add IA repository to `pluginManagement` in `settings.gradle.kts`

### "Hook class not found" when loading
**Fix**: Verify package names match paths:
- Group: `com.company`
- Module: `mymodule`
- Gateway hook path: `com.company.module.gateway.GatewayHook`
- File location: `gateway/src/main/java/com/company/module/gateway/GatewayHook.java`

### "Unsupported class file major version"
**Fix**: Set Java 17 toolchain in all subproject `build.gradle.kts` files

---

## Success Criteria

✓ Build completes without errors
✓ `.modl` file generated in `build/`
✓ Version appears in filename
✓ `module.xml` contains correct metadata
✓ Module is signed (META-INF files present)
✓ Installs in Gateway without errors
✓ Logs show hooks started successfully

---

## One-Line Test

```bash
./gradlew clean build && unzip -p build/*.modl module.xml && echo "✓ SUCCESS"
```

If you see the module.xml content and "✓ SUCCESS", it worked!

---

**Estimated Time**: 10 minutes
**Difficulty**: Beginner
**Prerequisites**: Java 17, Gradle 8+
**Result**: Working, signed Ignition 8.3 module

For detailed explanation, see: `IGNITION_MODULE_BUILD_GUIDE.md`
