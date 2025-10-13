# Ignition 8.3 SDK - Overview and Getting Started

## What is the Ignition SDK?

The Ignition Software Development Kit (SDK) is a framework that allows developers to extend and customize Ignition's core capabilities by creating custom modules. Many of Ignition's key features (Vision, Perspective, Reporting, device drivers) are themselves implemented as modules.

## Why Use the Ignition SDK?

- **Extend Ignition**: Add custom scripting functions, expression functions, components, tag providers, or device drivers
- **Leverage Expertise**: Integrate in-house algorithms or third-party Java libraries
- **Package and Share**: Distribute custom capabilities through the Ignition Module Development Community or as commercial modules
- **Customize Workflows**: Build specialized tools for your specific industry or use case

## Key Concepts

### Module Structure
A module is a `.modl` file - essentially a ZIP file containing:
- Compiled JAR files with your module code
- `module.xml` manifest file describing the module
- Optional license file and documentation

### Scopes
Ignition defines three execution scopes where module code can run:
- **Gateway (G)**: Server-side logic, database operations, OPC-UA devices, tag providers
- **Designer (D)**: Design-time tools, component palette additions, customizers
- **Client (C)**: Vision client runtime (legacy, for Vision modules only)

There's also a **Common** scope for code shared across multiple scopes.

### Hooks
Each scope requires a "hook" class - the entry point where Ignition loads and initializes your module:
- `GatewayModuleHook` (extends `AbstractGatewayModuleHook`)
- `DesignerModuleHook` (extends `AbstractDesignerModuleHook`)
- `ClientModuleHook` (extends `AbstractClientModuleHook`)

### Contexts
Each hook receives a context object providing access to Ignition platform services:
- `GatewayContext`: Access to database connections, tag providers, licensing, etc.
- `DesignerContext`: Access to project resources, RPC communication, etc.
- `VisionClientContext`: Access to client-side services (Vision only)

## Prerequisites

### Required Software
1. **Java JDK 17** (for Ignition 8.3)
   - Adoptium, Azul Zulu, or other TCK-tested JDKs work well
   - Download: https://adoptium.net/ or https://www.azul.com/downloads/

2. **Ignition 8.3.0+** running locally for testing
   - Download: https://inductiveautomation.com/downloads/
   - Configure for unsigned modules (development):
     ```
     # In ignition.conf, add:
     wrapper.java.additional.X=-Dignition.allowunsignedmodules=true
     ```

3. **Build Tool** (choose one):
   - **Gradle** (recommended, what IA uses internally)
   - **Maven** (also fully supported)

4. **IDE** (recommended):
   - IntelliJ IDEA (Community or Ultimate)
   - Eclipse
   - VS Code with Java extensions

5. **Git** for cloning examples and version control

### Required Knowledge
- **Java**: Strong foundation in Java programming, packages, classpaths, JAR files
- **Build Tools**: Basic understanding of Gradle or Maven
- **Ignition Platform**: Familiarity with Ignition configuration, tags, projects, etc.
- **Swing** (for Vision components): Java Swing framework and JavaBeans
- **React/TypeScript** (for Perspective components): Modern web development

## Quick Start Workflow

### 1. Set Up Development Environment
```bash
# Install JDK 17
# Install Ignition 8.3
# Install Gradle or Maven
# Install Git
# Install IntelliJ IDEA or your preferred IDE
```

### 2. Configure Ignition for Development
Edit `<ignition-install>/data/ignition.conf`:
```properties
wrapper.java.additional.7=-Dignition.allowunsignedmodules=true
```

Restart Ignition after making this change.

### 3. Clone SDK Examples
```bash
git clone https://github.com/inductiveautomation/ignition-sdk-examples.git
cd ignition-sdk-examples
```

### 4. Open Example in IDE
- Open IntelliJ IDEA
- File → Open
- Navigate to an example directory (e.g., `scripting-function`)
- Open the `build.gradle.kts` or `pom.xml` file
- Let IDE import dependencies automatically

### 5. Build the Module

**Using Gradle:**
```bash
# In project root directory
./gradlew build

# Or on Windows:
gradlew.bat build

# Module .modl file will be in build/libs/ directory
```

**Using Maven:**
```bash
mvn clean package

# Module .modl file will be in target/ directory
```

### 6. Install Module in Ignition
1. Open Ignition Gateway webpage (http://localhost:8088)
2. Navigate to Config → System → Modules
3. Scroll to bottom and click "Install or Upgrade a Module"
4. Select your built `.modl` file
5. Click "Install"
6. Module will be installed and activated

### 7. Test Your Module
- For scripting functions: Try in Script Console
- For components: Check Designer component palette
- For device drivers: Configure in OPC-UA → Device Connections
- Check Gateway logs for any errors: Config → System → Console

## Important SDK Resources

### Documentation
- **SDK Programmer's Guide**: https://www.sdk-docs.inductiveautomation.com/
- **Ignition User Manual**: https://docs.inductiveautomation.com/docs/8.3/intro
- **Java API Docs**: https://github.com/inductiveautomation/ignition-sdk-examples/wiki/Javadocs-&-Notable-API-Changes

### Code Examples
- **Official SDK Examples**: https://github.com/inductiveautomation/ignition-sdk-examples
- **Community Examples**: https://github.com/IgnitionModuleDevelopmentCommunity
- **Module Showcase**: https://inductiveautomation.com/exchange/

### Community
- **Module Development Forum**: https://forum.inductiveautomation.com/c/module-development/7
- **Inductive University**: https://inductiveuniversity.com/ (SDK courses available)

### Build Tools
- **Gradle Plugin**: https://github.com/inductiveautomation/ignition-module-tools
- **Maven Plugin**: https://nexus.inductiveautomation.com/repository/inductiveautomation-releases/

## Module Types You Can Build

1. **Scripting Functions**: Add custom Python scripting functions available in Gateway, Designer, or Vision Client
2. **Expression Functions**: Create custom expression functions for use in tags, bindings, etc.
3. **Perspective Components**: Build React-based components for Perspective module
4. **Vision Components**: Create Java Swing components for Vision module
5. **OPC-UA Device Drivers**: Implement custom device drivers for OPC-UA server
6. **Tag Providers**: Create custom tag provider implementations
7. **Gateway Webpage**: Add custom web pages to Gateway interface
8. **Alarm Notification**: Implement custom alarm notification types
9. **Database Operations**: Add custom database interactions and queries
10. **Report Components/Datasources**: Extend reporting capabilities

## Next Steps

1. Review the module architecture documentation to understand scopes, hooks, and contexts
2. Study the build system guide for your chosen tool (Gradle or Maven)
3. Pick an example module that matches your goals and study its structure
4. Start with simple modifications to examples before creating from scratch
5. Reference the appropriate guide for your module type (Perspective, Vision, OPC-UA, etc.)

## Common Development Pitfalls

### ❌ Don't:
- Forget to add dependencies with correct scope (compileOnly vs modlImplementation)
- Use blocking operations in hook lifecycle methods
- Ignore thread safety in Gateway-scoped modules
- Hardcode configuration values instead of using persistent records
- Skip proper cleanup in shutdown() methods

### ✅ Do:
- Extend Abstract hook classes instead of implementing interfaces directly
- Use provided platform services through context objects
- Implement proper lifecycle management (startup/shutdown)
- Test modules on fresh Ignition installations
- Follow module signing guidelines for production

## Version Compatibility Notes

- Ignition 8.3 requires JDK 17
- Ignition 8.1 uses JDK 11
- Ignition 7.9 uses JDK 8
- Always specify `requiredIgnitionVersion` in module descriptor
- Test modules against each Ignition version you intend to support
- Check API changelog for breaking changes between versions

## Development Best Practices

1. **Start Small**: Begin with simple examples, gradually add complexity
2. **Read Examples**: Study official examples before writing custom code
3. **Use Logging**: Implement proper logging with SLF4J
4. **Handle Errors**: Implement comprehensive error handling
5. **Document Code**: Add JavaDoc comments for public APIs
6. **Test Thoroughly**: Test in clean Ignition installation
7. **Follow Conventions**: Use reverse domain naming (com.company.module)
8. **Version Properly**: Use semantic versioning (MAJOR.MINOR.PATCH)

## Getting Help

1. **Check Documentation**: SDK Programmer's Guide and Javadocs first
2. **Search Forum**: Many questions already answered in forums
3. **Review Examples**: Look for similar functionality in example modules
4. **Ask Community**: Post detailed questions on Module Development Forum
5. **Stack Overflow**: For general Java/Gradle/Maven questions
6. **IA Support**: For platform bugs or API issues

## Ignition 8.3 New Features Relevant to SDK

- **Core Historian API**: Modules can implement custom historians
- **Event Streams Module**: Create custom sources and handlers
- **Git Integration**: Version control for projects
- **Deployment Modes**: Multiple environments on single server
- **Enhanced Container Support**: Better Docker/Kubernetes integration
- **Secrets Management**: Secure credential handling APIs
- **Project Resources API**: Enhanced project resource manipulation
