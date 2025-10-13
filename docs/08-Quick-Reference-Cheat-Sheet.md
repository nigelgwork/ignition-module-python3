# Ignition SDK - Quick Reference Cheat Sheet

## Project Setup

### Requirements
```bash
- JDK 17 (for Ignition 8.3)
- Ignition 8.3+ running locally
- Gradle or Maven
- IDE (IntelliJ/Eclipse/VS Code)
- Git
```

### Quick Start
```bash
# Clone examples
git clone https://github.com/inductiveautomation/ignition-sdk-examples.git

# Build module (Gradle)
./gradlew build

# Build module (Maven)
mvn clean package

# Module location:
# Gradle: build/libs/*.modl
# Maven: target/*.modl
```

### Enable Unsigned Modules
```properties
# In ignition.conf:
wrapper.java.additional.X=-Dignition.allowunsignedmodules=true
```

## Module Structure

### Scopes
| Scope | Letter | Purpose |
|-------|--------|---------|
| Gateway | G | Server-side logic, devices, tags |
| Designer | D | Design-time tools, component palette |
| Client | C | Vision client runtime (legacy) |
| Common | GD/GDC | Shared code between scopes |

### Hooks
```java
// Gateway
public class GatewayHook extends AbstractGatewayModuleHook {
    @Override public void setup(GatewayContext ctx) {}
    @Override public void startup(LicenseState lic) {}
    @Override public void shutdown() {}
}

// Designer
public class DesignerHook extends AbstractDesignerModuleHook {
    @Override public void setup(DesignerContext ctx) {}
    @Override public void startup(LicenseState lic) {}
    @Override public void shutdown() {}
}
```

### Module.xml Key Elements
```xml
<id>com.company.modulename</id>
<n>Module Display Name</n>
<version>1.0.0</version>
<requiredIgnitionVersion>8.3.0</requiredIgnitionVersion>
<requiredFrameworkVersion>8</requiredFrameworkVersion>

<jar scope="G">module-gateway.jar</jar>
<jar scope="D">module-designer.jar</jar>
<jar scope="GD">module-common.jar</jar>

<hook scope="G">com.company.module.gateway.GatewayHook</hook>
<hook scope="D">com.company.module.designer.DesignerHook</hook>
```

## Gradle Build

### Root build.gradle.kts
```kotlin
plugins {
    id("io.ia.sdk.modl") version("0.3.0")
}

ignitionModule {
    name.set("My Module")
    moduleId.set("com.company.mymodule")
    moduleVersion.set(version.toString())
    moduleDescription.set("Description")
    requiredIgnitionVersion.set("8.3.0")
    
    projectScopes.putAll(mapOf(
        ":gateway" to "G",
        ":designer" to "D",
        ":common" to "GD"
    ))
    
    hooks.putAll(mapOf(
        "com.company.module.gateway.GatewayHook" to "G",
        "com.company.module.designer.DesignerHook" to "D"
    ))
    
    skipModlSigning.set(true)
}
```

### Subproject Dependencies
```kotlin
dependencies {
    val sdkVersion = "8.3.0"
    
    // SDK (provided by platform)
    compileOnly("com.inductiveautomation.ignitionsdk:gateway-api:${sdkVersion}")
    
    // Third-party (included in module)
    modlImplementation("com.google.code.gson:gson:2.8.9")
    
    // Inter-project
    api(projects.common)
}
```

### Common Tasks
```bash
./gradlew build          # Build module
./gradlew clean          # Clean artifacts
./gradlew test           # Run tests
./gradlew tasks          # List all tasks
```

## Maven Build

### Root pom.xml
```xml
<properties>
    <ignition.version>8.3.0</ignition.version>
</properties>

<modules>
    <module>common</module>
    <module>gateway</module>
    <module>designer</module>
    <module>build</module>
</modules>

<repositories>
    <repository>
        <id>ia-releases</id>
        <url>https://nexus.inductiveautomation.com/repository/inductiveautomation-releases/</url>
    </repository>
</repositories>
```

### Build Module pom.xml
```xml
<plugin>
    <groupId>com.inductiveautomation.ignitionsdk</groupId>
    <artifactId>ignition-maven-plugin</artifactId>
    <version>1.2.2</version>
    <configuration>
        <moduleId>com.company.mymodule</moduleId>
        <moduleName>My Module</moduleName>
        <moduleVersion>${project.version}</moduleVersion>
        <requiredIgnitionVersion>8.3.0</requiredIgnitionVersion>
        <!-- More config... -->
    </configuration>
</plugin>
```

### Common Commands
```bash
mvn clean package        # Build module
mvn test                 # Run tests
mvn dependency:tree      # Show dependencies
```

## Scripting Functions

### Basic Function
```java
@ScriptFunction(docBundlePrefix = "ScriptFunctions")
public static double multiply(
        @ScriptArg("a") double a,
        @ScriptArg("b") double b) {
    return a * b;
}
```

### Register in Hook
```java
@Override
public void initializeScriptManager(ScriptManager manager) {
    super.initializeScriptManager(manager);
    manager.addScriptModule(
        "system.example",
        new ScriptFunctions(),
        new PropertiesFileDocProvider()
    );
}
```

### Python Usage
```python
result = system.example.multiply(5, 3)
```

## RPC Communication

### Interface (Common)
```java
public interface ModuleRPC {
    String getStatus();
    List<String> queryData(String query);
}
```

### Implementation (Gateway)
```java
public class ModuleRPCImpl implements ModuleRPC {
    @Override
    public String getStatus() {
        return "OK";
    }
}

// In GatewayHook:
@Override
public Object getRPCHandler(ClientReqSession session, String projectId) {
    return new ModuleRPCImpl();
}
```

### Usage (Designer)
```java
ModuleRPC rpc = ModuleRPCFactory.create(
    "com.company.module",
    ModuleRPC.class
);
String status = rpc.getStatus();
```

## Vision Components

### Component Class
```java
public class MyComponent extends AbstractVisionComponent {
    private String text = "Hello";
    
    public String getText() { return text; }
    
    public void setText(String text) {
        String old = this.text;
        this.text = text;
        firePropertyChange("text", old, text);
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawString(text, 10, 20);
    }
}
```

### BeanInfo Class
```java
public class MyComponentBeanInfo extends CommonBeanInfo {
    public MyComponentBeanInfo() {
        super(MyComponent.class, 
              DynamicPropertyProviderCustomizer.VALUE_DESCRIPTOR,
              StyleCustomizer.VALUE_DESCRIPTOR);
    }
    
    @Override
    protected void initProperties() throws IntrospectionException {
        super.initProperties();
        addProp("text", "Text", "Text to display", 
                CAT_DATA, PREFERRED_MASK | BOUND_MASK);
    }
}
```

### Register in Designer
```java
Palette palette = Palette.COMPONENT_PALETTE;
palette.getCategory("Custom").addComponent(
    new JavaBeanPaletteItem(
        MyComponent.class,
        MyComponentBeanInfo.class,
        "My Component"
    )
);
```

## Perspective Components

### Component Type ID
```java
public static final String COMPONENT_ID = "company.display.mycomponent";
```

### React Component
```typescript
export class MyComponent extends Component<ComponentProps<MyProps>, any> {
    render() {
        const { props, emit } = this.props;
        return (
            <div {...emit()}>
                {props.text}
            </div>
        );
    }
}
```

### Register in Gateway
```java
perspectiveContext.getComponentRegistry()
    .registerComponent(ComponentDescriptor);
```

### Props Schema (JSON)
```json
{
  "type": "object",
  "properties": {
    "text": {
      "type": "string",
      "default": "Hello"
    }
  }
}
```

## OPC-UA Device Drivers

### Device Settings
```java
public class DeviceSettings extends DeviceSettingsRecord {
    public static final StringField Hostname = 
        new StringField(META, "Hostname", SFieldFlags.SMANDATORY);
    public static final IntField Port = 
        new IntField(META, "Port").withDefault(502);
}
```

### Device Type
```java
public class CustomDeviceType extends DeviceType {
    @Override
    public String getTypeId() { return "CustomProtocol"; }
    
    @Override
    public Device createDevice(DeviceContext ctx, DeviceSettingsRecord settings) {
        return new CustomDevice(ctx, settings);
    }
}
```

### Device Implementation
```java
public class CustomDevice implements Device {
    @Override
    public void startup() {
        buildNodes();
        connect();
        startPolling();
    }
    
    @Override
    public void shutdown() {
        stopPolling();
        disconnect();
    }
}
```

### Register in Gateway
```java
public class GatewayHook extends AbstractDeviceModuleHook {
    @Override
    protected DeviceType getDeviceType() {
        return new CustomDeviceType();
    }
}
```

## Common Patterns

### Persistent Records
```java
public class Settings extends PersistentRecord {
    public static final RecordMeta<Settings> META = 
        new RecordMeta<>(Settings.class, "Settings");
    
    public static final StringField Name = 
        new StringField(META, "Name");
}

// Register in setup():
context.getSchemaUpdater().updatePersistentRecords(Settings.META);
```

### Extension Points
```java
// Scripting
context.getScriptManager().addScriptModule(...);

// Expressions
context.getExpressionFunctionManager().addFunction(...);

// Device Types
context.getDeviceTypeManager().register(...);

// Component Palette (Vision)
Palette.COMPONENT_PALETTE.addCategory(...);

// Component Registry (Perspective)
perspectiveContext.getComponentRegistry().registerComponent(...);
```

### Logging
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private final Logger logger = LoggerFactory.getLogger(getClass());

logger.info("Info message");
logger.warn("Warning message");
logger.error("Error message", exception);
```

## Testing

### Unit Test Template
```java
public class MyClassTest {
    @Test
    public void testMethod() {
        MyClass obj = new MyClass();
        assertEquals(expected, obj.method());
    }
}
```

### Run Tests
```bash
# Gradle
./gradlew test

# Maven
mvn test
```

## Troubleshooting

### Module won't load
- Check module.xml syntax
- Verify hook class paths
- Check dependencies
- Review Gateway logs

### Build fails
- Clean build: `./gradlew clean build`
- Check Java version: `java -version`
- Update dependencies
- Check network/proxy

### Component not in palette
- Check BeanInfo class name
- Verify registration
- Restart Designer
- Check logs

## Resources

```
SDK Docs:     https://www.sdk-docs.inductiveautomation.com/
Examples:     https://github.com/inductiveautomation/ignition-sdk-examples
Forum:        https://forum.inductiveautomation.com/c/module-development/7
Javadocs:     Check SDK examples wiki
User Manual:  https://docs.inductiveautomation.com/docs/8.3/intro
```

## Version Requirements

| Ignition | JDK | Framework |
|----------|-----|-----------|
| 8.3.x    | 17  | 8         |
| 8.1.x    | 11  | 8         |
| 7.9.x    | 8   | 7         |

## Common Issues

| Issue | Solution |
|-------|----------|
| Unsigned modules | Add `-Dignition.allowunsignedmodules=true` |
| Dependencies not found | Check repository URLs |
| Module won't uninstall | Restart Gateway |
| Designer not seeing changes | Clear designer cache |
| OOM errors | Increase Gateway heap size |
