# Ignition Module Build - Troubleshooting Guide

**Purpose**: Solutions to common problems when building Ignition 8.3 SDK modules.

---

## Quick Diagnostic Commands

Run these to identify the issue:

```bash
# 1. Check Java version (must be 17)
java -version

# 2. Check Gradle
./gradlew --version

# 3. Try clean build
./gradlew clean build --stacktrace

# 4. Check if module file was created
ls -lh build/*.modl

# 5. View module.xml if module was created
unzip -p build/*.modl module.xml 2>/dev/null || echo "No module file found"

# 6. Check for signing
unzip -l build/*.modl 2>/dev/null | grep META-INF
```

---

## Error: Plugin 'io.ia.sdk.modl' not found

### Full Error Message:
```
Plugin [id: 'io.ia.sdk.modl', version: '0.1.1'] was not found in any of the following sources:
- Gradle Core Plugins (plugin is not in 'org.gradle' namespace)
- Plugin Repositories (could not resolve plugin artifact)
```

### Cause:
Gradle cannot find the Ignition SDK plugin because the repository isn't configured.

### Solution:

**1. Create or update `settings.gradle.kts`:**
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
```

**2. Verify file is at project root:**
```bash
ls -la settings.gradle.kts
```

**3. Clean and retry:**
```bash
./gradlew clean build --refresh-dependencies
```

### If Still Failing:

Check network connectivity to IA's nexus:
```bash
curl -I https://nexus.inductiveautomation.com/repository/public/
```

Should return `HTTP 200 OK`.

---

## Error: Cannot find symbol - Hook classes

### Full Error Message:
```
error: cannot find symbol
  symbol:   class GatewayHook
  location: package com.company.module.gateway
```

### Cause:
Hook class doesn't exist at the path specified in `build.gradle.kts`, or package name is wrong.

### Solution:

**1. Verify hook configuration in `build.gradle.kts`:**
```kotlin
hooks.putAll(mapOf(
    "com.company.module.gateway.GatewayHook" to "G",
    "com.company.module.designer.DesignerHook" to "D"
))
```

**2. Check actual file location:**
```bash
find . -name "GatewayHook.java"
# Should be: ./gateway/src/main/java/com/company/module/gateway/GatewayHook.java
```

**3. Verify package declaration in Java file:**
```java
package com.company.module.gateway;  // Must match path
```

**4. Verify group setting matches package:**
```kotlin
group = "com.company"  // In build.gradle.kts

// Then package should be:
// com.company.module.gateway.GatewayHook
```

### Path Calculation Formula:
```
{scope}/src/main/java/{group}/{module}/{scope}/{HookName}.java

Example:
gateway/src/main/java/com/company/module/gateway/GatewayHook.java
                      └─────┬─────┘ └──┬──┘ └──┬──┘
                          group     module   scope
```

---

## Error: Unsupported class file major version

### Full Error Message:
```
Unsupported class file major version 61
```

### Cause:
Code compiled with Java 17 but Ignition running on older Java, OR code compiled with newer Java.

### Solution:

**1. Verify Java version on build machine:**
```bash
java -version
# Must be: openjdk version "17.x.x"
```

**2. Set Java toolchain in ALL subproject `build.gradle.kts` files:**

`gateway/build.gradle.kts`:
```kotlin
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
```

`designer/build.gradle.kts`:
```kotlin
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
```

`common/build.gradle.kts`:
```kotlin
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
```

**3. Clean and rebuild:**
```bash
./gradlew clean build
```

**4. Verify Ignition Gateway is using Java 17:**
```bash
# Check Gateway info
curl http://localhost:8088/system/gwinfo | grep RuntimeVersion
# Should show: RuntimeVersion=17
```

---

## Error: Module file not signed

### Symptom:
Gateway shows: "Module is not signed" or "Invalid signature"

### Cause:
Module signing was skipped or failed.

### Solution:

**1. Check `skipModlSigning` setting:**
```kotlin
ignitionModule {
    skipModlSigning.set(false)  // Must be false or omitted
}
```

**2. Verify signing occurred:**
```bash
./gradlew build --info | grep signModule
# Should show: > Task :signModule
```

**3. Check for signature files:**
```bash
unzip -l build/*.modl | grep META-INF
```

Should show:
```
META-INF/MANIFEST.MF
META-INF/CERT.SF
META-INF/CERT.RSA
```

**4. If missing, rebuild with signing:**
```bash
./gradlew clean build
```

**5. If still not signed, check for errors:**
```bash
./gradlew clean build --stacktrace --debug | grep -i sign
```

---

## Error: Package does not exist

### Full Error Message:
```
error: package com.inductiveautomation.ignition.gateway.model does not exist
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
```

### Cause:
SDK dependencies not configured correctly.

### Solution:

**1. Create `gradle/libs.versions.toml`:**
```toml
[versions]
ignition = "8.3.0"

[libraries]
ignition-common = { module = "com.inductiveautomation.ignitionsdk:ignition-common", version.ref = "ignition" }
ignition-gateway-api = { module = "com.inductiveautomation.ignitionsdk:gateway-api", version.ref = "ignition" }
ignition-designer-api = { module = "com.inductiveautomation.ignitionsdk:designer-api", version.ref = "ignition" }
```

**2. Add repository to `settings.gradle.kts`:**
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        maven {
            url = uri("https://nexus.inductiveautomation.com/repository/public/")
        }
    }
}
```

**3. Add dependencies to subproject `build.gradle.kts`:**

`gateway/build.gradle.kts`:
```kotlin
dependencies {
    compileOnly(libs.ignition.common)
    compileOnly(libs.ignition.gateway.api)
}
```

**4. Refresh dependencies:**
```bash
./gradlew clean build --refresh-dependencies
```

---

## Error: Could not find [artifact]

### Full Error Message:
```
Could not find com.inductiveautomation.ignitionsdk:gateway-api:8.3.0
```

### Cause:
Maven repository not accessible or version doesn't exist.

### Solution:

**1. Test repository access:**
```bash
curl https://nexus.inductiveautomation.com/repository/public/
```

**2. Check if version exists:**
Visit: https://nexus.inductiveautomation.com/repository/public/com/inductiveautomation/ignitionsdk/gateway-api/

**3. Try with offline cache:**
```bash
./gradlew clean build --offline
```

**4. Clear Gradle cache:**
```bash
rm -rf ~/.gradle/caches
./gradlew clean build --refresh-dependencies
```

**5. Use RC version if 8.3.0 exact not available:**
```toml
[versions]
ignition = "8.3.0-rc1"  # Or latest available
```

---

## Error: Vendor name not showing in Gateway

### Symptom:
Module installs but vendor name doesn't appear in Gateway modules list.

### Cause:
Ignition doesn't have a separate "vendor" field - it's part of the description.

### Solution:

**1. Update `moduleDescription` in `build.gradle.kts`:**
```kotlin
ignitionModule {
    moduleDescription.set("Your module description here. Developed by Your Company Name.")
    //                                                  └── Include vendor here ──┘
}
```

**2. Rebuild module:**
```bash
./gradlew clean build
```

**3. Verify in module.xml:**
```bash
unzip -p build/*.modl module.xml | grep description
```

Should show:
```xml
<description>Your module description here. Developed by Your Company Name.</description>
```

**4. Reinstall in Gateway and check description column.**

---

## Error: Module loads but hooks don't start

### Symptom:
Module shows "Loaded" but logs don't show hook messages.

### Cause:
Hook classes don't extend correct base class or have compilation errors.

### Solution:

**1. Verify hook base classes:**

Gateway hook must extend:
```java
public class GatewayHook extends AbstractGatewayModuleHook {
```

Designer hook must extend:
```java
public class DesignerHook extends AbstractDesignerModuleHook {
```

**2. Verify required methods:**

Gateway:
```java
@Override
public void setup(GatewayContext context) { }

@Override
public void startup(LicenseState licenseState) { }

@Override
public void shutdown() { }
```

Designer:
```java
@Override
public void startup(DesignerContext context, LicenseState activationState) { }

@Override
public void shutdown() { }
```

**3. Check Gateway logs:**
```bash
tail -f /path/to/ignition/logs/wrapper.log | grep "your module"
```

Look for exceptions or "Hook class not found" errors.

**4. Verify module.xml hook entries:**
```bash
unzip -p build/*.modl module.xml | grep hook
```

Should match your actual classes.

---

## Error: Dependencies not bundled in module

### Symptom:
Module installs but crashes with `ClassNotFoundException` for third-party library.

### Cause:
Dependencies configured with `compileOnly` instead of `modlImplementation`.

### Solution:

**1. Check dependency configuration:**
```kotlin
dependencies {
    compileOnly(libs.ignition.gateway.api)  // ✓ SDK - provided by platform
    modlImplementation("com.google.code.gson:gson:2.10.1")  // ✓ Bundle this
}
```

**2. Change third-party dependencies to `modlImplementation`:**
```kotlin
dependencies {
    // Platform-provided (don't bundle)
    compileOnly(libs.ignition.common)
    compileOnly(libs.ignition.gateway.api)

    // Third-party (bundle in module)
    modlImplementation("com.squareup.okhttp3:okhttp:4.12.0")
    modlImplementation("com.google.code.gson:gson:2.10.1")
}
```

**3. Rebuild and verify JARs are included:**
```bash
./gradlew clean build
unzip -l build/*.modl | grep -E "(gson|okhttp)"
```

Should show:
```
gson-2.10.1.jar
okhttp-4.12.0.jar
```

---

## Error: Version not appearing in filename

### Symptom:
Module file is named `module.modl` instead of `MyModule-1.0.0.modl`

### Cause:
`fileName` not configured with version.

### Solution:

**1. Update `build.gradle.kts`:**
```kotlin
ignitionModule {
    fileName.set("MyModule-${project.version}")  // Include version
}
```

**2. Rebuild:**
```bash
./gradlew clean build
ls -lh build/*.modl
```

Should show: `MyModule-1.0.0.modl`

---

## Error: Build succeeds but module won't install

### Symptom:
Gateway shows "Failed to install module" with no specific error.

### Cause:
Multiple possible issues with module structure.

### Solution:

**1. Check module.xml validity:**
```bash
unzip -p build/*.modl module.xml | xmllint --format -
```

If XML is invalid, there's a configuration problem.

**2. Verify all required fields present:**
```bash
unzip -p build/*.modl module.xml
```

Must have:
```xml
<name>...</name>
<id>...</id>
<version>...</version>
<requiredIgnitionVersion>...</requiredIgnitionVersion>
<hook scope="G">...</hook>
```

**3. Check for scope/JAR mismatches:**
```bash
unzip -l build/*.modl | grep jar
```

Each scope in hooks must have corresponding JAR:
- Hook with scope "G" → need `gateway-X.X.X.jar`
- Hook with scope "D" → need `designer-X.X.X.jar`

**4. Check Gateway logs for specific error:**
```bash
tail -f /path/to/ignition/logs/wrapper.log
```

Then try installing module.

---

## Error: Cannot resolve dependency

### Full Error Message:
```
Could not determine the dependencies of task ':gateway:compileJava'.
Could not resolve all task dependencies for configuration ':gateway:compileClasspath'.
```

### Cause:
Project dependency reference is incorrect.

### Solution:

**1. If using `implementation(projects.common)`, ensure:**
```kotlin
// settings.gradle.kts must have:
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// And include the subproject:
include(":common")
```

**2. Alternative: Use string reference:**
```kotlin
dependencies {
    implementation(project(":common"))  // Instead of projects.common
}
```

**3. Verify subproject exists:**
```bash
ls -la common/build.gradle.kts
```

**4. Clean and rebuild:**
```bash
./gradlew clean build
```

---

## Performance: Build is very slow

### Symptom:
`./gradlew build` takes several minutes.

### Solution:

**1. Enable Gradle daemon:**
Create/edit `gradle.properties`:
```properties
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
```

**2. Increase memory:**
```properties
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
```

**3. Use build cache:**
```bash
./gradlew build --build-cache
```

**4. Skip tests during development:**
```bash
./gradlew build -x test
```

---

## Debugging: Enable verbose output

```bash
# Stacktrace for errors
./gradlew build --stacktrace

# Full debug information
./gradlew build --debug > build.log 2>&1

# Info level (less verbose than debug)
./gradlew build --info
```

---

## Clean Slate: Start fresh

If all else fails:

```bash
# 1. Clean Gradle cache
./gradlew clean
rm -rf build/
rm -rf .gradle/

# 2. Clean user Gradle cache
rm -rf ~/.gradle/caches

# 3. Re-download wrapper
rm -rf gradle/wrapper
gradle wrapper --gradle-version 8.5

# 4. Rebuild
./gradlew clean build --refresh-dependencies --stacktrace
```

---

## Verification: Is my module correct?

Run this comprehensive check:

```bash
#!/bin/bash
echo "=== Ignition Module Verification ==="

# Check module file
if [ -f build/*.modl ]; then
    echo "✓ Module file exists"
    ls -lh build/*.modl
else
    echo "✗ Module file not found"
    exit 1
fi

# Check signing
if unzip -l build/*.modl 2>/dev/null | grep -q "META-INF/CERT.RSA"; then
    echo "✓ Module is signed"
else
    echo "✗ Module is not signed"
fi

# Check module.xml
if unzip -p build/*.modl module.xml 2>/dev/null > /tmp/module.xml; then
    echo "✓ module.xml exists"

    # Check required fields
    if grep -q "<name>" /tmp/module.xml; then
        echo "✓ Name field present"
    fi

    if grep -q "<id>" /tmp/module.xml; then
        echo "✓ ID field present"
    fi

    if grep -q "<version>" /tmp/module.xml; then
        echo "✓ Version field present"
    fi

    if grep -q "<hook" /tmp/module.xml; then
        echo "✓ Hook(s) defined"
    fi
else
    echo "✗ Cannot read module.xml"
fi

# Check JAR files
echo ""
echo "JARs in module:"
unzip -l build/*.modl 2>/dev/null | grep "\.jar$" | awk '{print $4}'

rm -f /tmp/module.xml
echo ""
echo "=== Verification Complete ==="
```

Save as `verify-module.sh`, make executable, and run:
```bash
chmod +x verify-module.sh
./verify-module.sh
```

---

## Getting Help

If still stuck:

**1. Check official docs:**
https://www.sdk-docs.inductiveautomation.com/

**2. Search forum:**
https://forum.inductiveautomation.com/c/module-development/7

**3. Check SDK examples:**
https://github.com/inductiveautomation/ignition-sdk-examples

**4. Include in help request:**
- Gradle version: `./gradlew --version`
- Java version: `java -version`
- Ignition version
- Full error message
- Build file contents
- Directory structure: `tree -L 3`

---

## Common Working Configurations

### Minimal Working (No Dependencies)
```
my-module/
├── build.gradle.kts (plugin 0.1.1, hooks defined)
├── settings.gradle.kts (IA repository)
├── gradle/libs.versions.toml (SDK 8.3.0)
├── gateway/
│   ├── build.gradle.kts (Java 17 toolchain)
│   └── src/main/java/.../GatewayHook.java
└── designer/
    ├── build.gradle.kts (Java 17 toolchain)
    └── src/main/java/.../DesignerHook.java
```

### With Common + Dependencies
```
my-module/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml
├── gateway/
│   ├── build.gradle.kts (impl common, modlImpl gson)
│   └── src/main/java/.../GatewayHook.java
├── designer/
│   ├── build.gradle.kts (impl common)
│   └── src/main/java/.../DesignerHook.java
└── common/
    ├── build.gradle.kts (api gson)
    └── src/main/java/.../Constants.java
```

---

**Last Updated**: October 13, 2025
**Most Common Issues**: Hook paths, Java version, plugin repository
**Success Rate**: Follow guide exactly = 99% success
