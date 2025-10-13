# INSTRUCTIONS FOR CLAUDE CODE ENVIRONMENTS

## Purpose
These instructions enable Claude Code to effectively develop, test, and debug Ignition 8.3 SDK modules. This document provides workflows, best practices, and automation patterns for module development.

## Prerequisites Verification

Before starting any Ignition SDK work, verify:

```bash
# Check Java version (must be 17 for Ignition 8.3)
java -version

# Check Ignition is running
curl -s http://localhost:8088/system/gwinfo | grep version

# Check Gradle/Maven
./gradlew --version  # OR
mvn --version

# Verify knowledge base location
ls /mnt/user-data/uploads/ | grep ignition
```

## Knowledge Base Structure

The Ignition SDK knowledge base consists of:

1. **01-SDK-Overview-Getting-Started.md** - Core concepts, setup, prerequisites
2. **02-Module-Architecture-Structure.md** - Scopes, hooks, lifecycle, structure
3. **03-Build-Systems-Gradle-Maven.md** - Build configuration and commands
4. **04-Perspective-Component-Development.md** - React/TypeScript components
5. **05-Vision-Component-Development.md** - Java Swing components
6. **06-OPC-UA-Device-Driver-Development.md** - Custom device drivers
7. **07-Scripting-Functions-RPC-Communication.md** - Python functions and RPC
8. **08-Quick-Reference-Cheat-Sheet.md** - Quick lookup reference

**Always consult relevant knowledge base files before implementing features.**

## Standard Workflows

### 1. Create New Module from Scratch

```bash
# Step 1: Clone examples for reference
git clone https://github.com/inductiveautomation/ignition-sdk-examples.git

# Step 2: Find similar example
cd ignition-sdk-examples
ls -la  # List available examples

# Step 3: Copy and modify example
cp -r scripting-function my-new-module
cd my-new-module

# Step 4: Update module configuration
# - Edit build.gradle.kts (or pom.xml)
# - Change module ID, name, version
# - Update package names in Java files

# Step 5: Build
./gradlew clean build  # OR: mvn clean package

# Step 6: Install module
# Open http://localhost:8088
# Config → Modules → Install module (.modl file from build/libs/)
```

### 2. Analyze Existing Module

```bash
# Step 1: View module structure
tree -L 3 .

# Step 2: Identify scopes
ls -la */src/main/java/*/

# Step 3: Locate hooks
find . -name "*Hook.java"

# Step 4: Understand configuration
cat build.gradle.kts  # OR: cat pom.xml

# Step 5: Read knowledge base for detected patterns
# Example: If Perspective components found, read file 04
```

### 3. Add New Scripting Function

```bash
# Step 1: Read knowledge base
cat /path/to/07-Scripting-Functions-RPC-Communication.md

# Step 2: Create function class
# - Add @ScriptFunction annotations
# - Implement business logic
# - Create .properties file for documentation

# Step 3: Register in hook
# - Locate GatewayHook.java (or DesignerHook.java)
# - Override initializeScriptManager()
# - Register function module

# Step 4: Build and test
./gradlew build
# Install module
# Test in Ignition Script Console
```

### 4. Develop Perspective Component

```bash
# Step 1: Review Perspective guide
cat /path/to/04-Perspective-Component-Development.md

# Step 2: Set up web build environment
cd web
npm install

# Step 3: Create component files
# - TypeScript component in packages/client/typescript/components/
# - Props JSON schema
# - Registration in index.ts

# Step 4: Create Java integration
# - ComponentMeta.java in common/
# - Register in GatewayHook

# Step 5: Build
npm run build          # Build web components
cd ..
./gradlew build        # Build module

# Step 6: Test
# Install module, check Designer component palette
```

### 5. Debug Module Issues

```bash
# Step 1: Check Gateway logs
tail -f /path/to/ignition/logs/wrapper.log

# Step 2: Enable debug logging
# Gateway Config → System → Console → Logging
# Set package to DEBUG level

# Step 3: Reproduce issue with logging
# Add strategic logger statements:
# logger.debug("Variable value: {}", value);

# Step 4: Rebuild and reinstall
./gradlew clean build
# Uninstall old module
# Install new module
# Check logs

# Step 5: Use IntelliJ remote debugging (if needed)
# Add to ignition.conf:
# wrapper.java.additional.X=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
# Connect IntelliJ debugger to localhost:5005
```

## Code Generation Templates

### Create Gateway Hook
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
        logger.info("Module setup starting");
    }
    
    @Override
    public void startup(LicenseState licenseState) {
        logger.info("Module startup");
    }
    
    @Override
    public void shutdown() {
        logger.info("Module shutdown");
    }
}
```

### Create Scripting Function
```java
package com.company.module.functions;

import com.inductiveautomation.ignition.common.script.hints.ScriptArg;
import com.inductiveautomation.ignition.common.script.hints.ScriptFunction;

public class CustomFunctions {
    @ScriptFunction(docBundlePrefix = "CustomFunctions")
    public static String processData(
            @ScriptArg("input") String input) {
        // Implementation
        return "Processed: " + input;
    }
}
```

### Create Gradle Build Config
```kotlin
plugins {
    id("io.ia.sdk.modl") version("0.3.0")
}

ignitionModule {
    name.set("Custom Module")
    moduleId.set("com.company.custommodule")
    moduleVersion.set("1.0.0")
    moduleDescription.set("Module description")
    requiredIgnitionVersion.set("8.3.0")
    
    projectScopes.putAll(mapOf(
        ":gateway" to "G",
        ":common" to "G"
    ))
    
    hooks.putAll(mapOf(
        "com.company.custommodule.gateway.GatewayHook" to "G"
    ))
    
    skipModlSigning.set(true)
}
```

## Testing Procedures

### Manual Testing Checklist

1. **Build Verification**
   ```bash
   ./gradlew clean build
   # Verify .modl file created
   ls -lh build/libs/*.modl
   ```

2. **Installation Test**
   - Gateway Config → Modules
   - Install module
   - Check status = "Running"
   - Review Gateway logs for errors

3. **Functionality Test**
   - Scripting: Test in Script Console
   - Components: Check Designer palette
   - Devices: Check OPC-UA connections
   - RPC: Test Designer-Gateway communication

4. **Uninstall Test**
   - Uninstall module
   - Verify clean removal
   - Check for memory leaks in logs

### Automated Testing

```java
// Unit test template
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MyClassTest {
    @Test
    public void testMethod() {
        MyClass instance = new MyClass();
        assertEquals(expected, instance.method());
    }
}

// Run tests
./gradlew test
```

## Common Issues and Solutions

### Issue: Module won't load
**Diagnosis:**
```bash
# Check Gateway logs
tail -100 logs/wrapper.log | grep -i error

# Verify module.xml
unzip -p build/libs/*.modl module.xml
```
**Solutions:**
- Verify hook class paths
- Check dependency versions
- Ensure all required JARs included
- Review module.xml syntax

### Issue: Scripting functions not appearing
**Diagnosis:**
```bash
# Check registration
grep -r "initializeScriptManager" .
```
**Solutions:**
- Verify ScriptManager initialization
- Check module name registration
- Restart Designer
- Clear Designer cache

### Issue: Build fails
**Diagnosis:**
```bash
./gradlew build --stacktrace --info
```
**Solutions:**
- Clean build: `./gradlew clean`
- Update dependencies
- Check Java version
- Verify repository access

### Issue: Components not in palette
**Diagnosis:**
```bash
# Check hook registration
grep -r "ComponentRegistry\|Palette" .
```
**Solutions:**
- Verify component registration
- Check BeanInfo class (Vision)
- Verify component descriptor (Perspective)
- Restart Designer

## Best Practices for Claude Code

### 1. Always Start with Knowledge Base
- Read relevant guide before implementing
- Reference examples from SDK repository
- Follow established patterns

### 2. Incremental Development
- Build and test frequently
- Commit working code often
- One feature at a time

### 3. Comprehensive Logging
```java
// Add logging at key points
logger.info("Starting operation");
logger.debug("Variable state: {}", variable);
logger.error("Operation failed", exception);
```

### 4. Error Handling
```java
// Always handle exceptions
try {
    // Risky operation
} catch (Exception e) {
    logger.error("Error description", e);
    // Graceful degradation
}
```

### 5. Resource Cleanup
```java
@Override
public void shutdown() {
    // Always clean up
    if (executor != null) {
        executor.shutdown();
    }
    // Close connections
    // Stop threads
    // Release resources
}
```

### 6. Thread Safety
```java
// Use synchronized or concurrent collections
private final ConcurrentHashMap<String, Object> cache = 
    new ConcurrentHashMap<>();

// Or synchronize methods
public synchronized void updateState() {
    // Thread-safe operation
}
```

### 7. Documentation
```java
/**
 * Comprehensive JavaDoc for all public methods
 * 
 * @param input The input parameter
 * @return The result
 * @throws Exception If operation fails
 */
public String method(String input) throws Exception {
    // Implementation
}
```

## File Organization

### Recommended Project Structure
```
module-root/
├── .gitignore
├── README.md
├── build.gradle.kts (or pom.xml)
├── settings.gradle.kts
├── common/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── java/
│       └── resources/
├── gateway/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── java/
│       └── resources/
├── designer/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── java/
│       └── resources/
└── web/ (for Perspective)
    ├── package.json
    └── packages/
```

### Essential .gitignore
```gitignore
# Build outputs
build/
target/
*.modl

# IDE
.idea/
.vscode/
*.iml

# Gradle
.gradle/

# Node (for Perspective)
node_modules/
dist/

# Logs
*.log
```

## Integration with Ignition

### Gateway API Access
```java
// Access Gateway services
context.getDatabaseManager()
context.getScriptManager()
context.getTagManager()
context.getAlarmManager()
context.getProjectManager()
```

### Designer API Access
```java
// Access Designer services
context.getProject()
context.getDesignerUI()
context.getTagBrowseTree()
```

### Module Dependencies
```kotlin
// Gradle
dependencies {
    // Depend on other modules
    compileOnly("com.inductiveautomation.ignitionsdk:vision-common:8.3.0")
    compileOnly("com.inductiveautomation.ignitionsdk:perspective-common:8.3.0")
}
```

## Performance Optimization

### 1. Minimize Gateway Load
- Use scheduled executors for polling
- Cache expensive computations
- Batch database operations
- Use async operations where possible

### 2. Efficient Node Updates
```java
// Batch OPC-UA node updates
List<UaVariableNode> nodes = getNodes();
for (UaVariableNode node : nodes) {
    node.setValue(new DataValue(new Variant(value)));
}
// Updates happen in batch
```

### 3. Memory Management
```java
// Use weak references for caches
WeakHashMap<String, Object> cache = new WeakHashMap<>();

// Clear collections
list.clear();

// Null large objects
largeObject = null;
```

## Deployment Checklist

### Development to Production

1. **Code Review**
   - Check for hardcoded values
   - Verify error handling
   - Review security implications
   - Ensure thread safety

2. **Testing**
   - Unit tests pass
   - Integration tests pass
   - Manual testing complete
   - Performance testing done

3. **Documentation**
   - README updated
   - API docs generated
   - Installation instructions
   - Configuration guide

4. **Build Configuration**
   - Enable module signing
   - Set production version
   - Remove debug code
   - Optimize logging

5. **Release**
   - Tag version in Git
   - Build signed module
   - Test signed module
   - Distribute module

## Conclusion

This instruction set provides comprehensive guidance for developing Ignition SDK modules using Claude Code. Always:

1. **Reference knowledge base first**
2. **Follow established patterns**
3. **Test incrementally**
4. **Document thoroughly**
5. **Handle errors gracefully**
6. **Clean up resources**
7. **Log appropriately**

For specific implementations, consult the relevant knowledge base file and SDK examples.
