# Ignition SDK - Build Systems (Gradle & Maven)

## Overview

The Ignition SDK supports both Gradle and Maven as build tools. Inductive Automation uses Gradle internally and provides first-class support for both systems.

**Gradle** - Modern, flexible, programming-language-based configuration
**Maven** - Mature, XML-based, convention-over-configuration approach

## Gradle Build System

### Why Gradle?

- Used by Inductive Automation for Ignition itself
- Faster builds with incremental compilation
- More flexible configuration
- Kotlin or Groovy DSL
- Better IDE integration
- Modern caching and parallel execution

### Gradle Project Structure

```
my-module/
├── settings.gradle.kts          # Project settings
├── build.gradle.kts             # Root build configuration
├── gradle/
│   └── wrapper/                 # Gradle wrapper (don't need Gradle installed)
├── gradlew                      # Unix wrapper script
├── gradlew.bat                  # Windows wrapper script
├── common/
│   ├── build.gradle.kts        # Common subproject build
│   └── src/main/java/...
├── gateway/
│   ├── build.gradle.kts        # Gateway subproject build
│   └── src/main/java/...
└── designer/
    ├── build.gradle.kts        # Designer subproject build
    └── src/main/java/...
```

### Root build.gradle.kts

```kotlin
plugins {
    id("io.ia.sdk.modl") version("0.3.0")
}

// Configure module metadata
ignitionModule {
    // Module name (required)
    name.set("My Custom Module")
    
    // Module ID - unique identifier (required)
    // Use reverse domain notation
    moduleId.set("com.company.mymodule")
    
    // Module version (required)
    moduleVersion.set(version.toString())
    
    // Short description
    moduleDescription.set("Custom functionality for Ignition")
    
    // Documentation and license
    license.set("license.html")
    documentationIndex.set("doc/index.html")
    
    // Required Ignition version
    requiredIgnitionVersion.set("8.3.0")
    requiredFrameworkVersion.set("8")
    
    // Project scopes map
    // Key = project path, Value = scope letter(s)
    projectScopes.putAll(mapOf(
        ":gateway" to "G",
        ":designer" to "D",
        ":client" to "C",
        ":common" to "GD"
    ))
    
    // Module dependencies
    moduleDependencies.putAll(mapOf(
        "com.inductiveautomation.vision" to "D",
        "com.inductiveautomation.perspective" to "GD"
    ))
    
    // Hook classes
    hooks.putAll(mapOf(
        "com.company.mymodule.gateway.GatewayHook" to "G",
        "com.company.mymodule.designer.DesignerHook" to "D",
        "com.company.mymodule.client.ClientHook" to "C"
    ))
    
    // Skip module signing for development
    skipModlSigning.set(true)
    
    // Optional: Apply to Designer and Workstation (default: both)
    applyToDesigner.set(true)
    applyToWorkstation.set(true)
}

// Configure all projects
allprojects {
    group = "com.company"
    version = "1.0.0-SNAPSHOT"
}

// Configure subprojects
subprojects {
    apply(plugin = "java-library")
    
    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
    
    dependencies {
        val sdkVersion = "8.3.0"
        
        // SDK dependencies (provided by platform)
        compileOnly("com.inductiveautomation.ignitionsdk:ignition-common:${sdkVersion}")
        
        // Testing dependencies
        testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
        testImplementation("org.mockito:mockito-core:4.8.0")
    }
    
    tasks.test {
        useJUnitPlatform()
    }
}
```

### settings.gradle.kts

```kotlin
rootProject.name = "my-module"

// Define the Inductive Automation repository
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://nexus.inductiveautomation.com/repository/public")
        }
    }
}

// Configure dependency resolution
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://nexus.inductiveautomation.com/repository/public")
        }
    }
}

// Include subprojects
include(":common")
include(":gateway")
include(":designer")
include(":client")
```

### Subproject build.gradle.kts Example (Gateway)

```kotlin
dependencies {
    val sdkVersion = "8.3.0"
    
    // Depend on common subproject
    api(projects.common)
    
    // Gateway-specific SDK dependencies
    compileOnly("com.inductiveautomation.ignitionsdk:gateway-api:${sdkVersion}")
    compileOnly("com.inductiveautomation.ignitionsdk:ignition-common:${sdkVersion}")
    
    // Third-party libraries to include in module
    modlImplementation("com.google.code.gson:gson:2.8.9")
    modlImplementation("org.apache.httpcomponents:httpclient:4.5.13")
    
    // Additional dependencies (not included in module)
    implementation("org.slf4j:slf4j-api:1.7.36")
}
```

### Key Gradle Concepts

#### Dependency Configurations

1. **compileOnly**: Available at compile time, provided by platform at runtime
   - Use for all Ignition SDK dependencies
   
2. **modlImplementation**: Included in the module .modl file
   - Use for third-party libraries your module needs
   
3. **modlApi**: Included in module and exposed to other modules
   - Use sparingly, only for transitive dependencies
   
4. **api**: Compile-time dependency exposed to dependents
   - Use for subproject dependencies (like `:common`)
   
5. **implementation**: Compile-time dependency, not exposed
   - Use for internal dependencies

6. **testImplementation**: Test-only dependencies
   - JUnit, Mockito, etc.

#### Common Gradle Tasks

```bash
# Build the module
./gradlew build

# Clean build artifacts
./gradlew clean

# Build without tests
./gradlew build -x test

# Run tests only
./gradlew test

# List all tasks
./gradlew tasks

# Deploy to local Gateway (if configured)
./gradlew deployModl

# Build and watch for changes
./gradlew build --continuous
```

### Gradle Configuration Tips

#### Custom Repository

```kotlin
repositories {
    maven {
        url = uri("https://your-private-repo.com/maven")
        credentials {
            username = findProperty("repo.username") as String?
            password = findProperty("repo.password") as String?
        }
    }
}
```

#### Custom Tasks

```kotlin
tasks.register("printVersion") {
    doLast {
        println("Module version: ${project.version}")
    }
}
```

#### Conditional Dependencies

```kotlin
if (project.hasProperty("includeDebugTools")) {
    dependencies {
        modlImplementation("com.company:debug-tools:1.0.0")
    }
}
```

## Maven Build System

### Why Maven?

- Mature, well-established
- XML-based configuration
- Strong convention over configuration
- Extensive plugin ecosystem
- Familiar to many Java developers

### Maven Project Structure

```
my-module/
├── pom.xml                      # Root POM
├── common/
│   ├── pom.xml                 # Common submodule POM
│   └── src/main/java/...
├── gateway/
│   ├── pom.xml                 # Gateway submodule POM
│   └── src/main/java/...
├── designer/
│   ├── pom.xml                 # Designer submodule POM
│   └── src/main/java/...
└── build/
    └── pom.xml                 # Build aggregator POM
```

### Root pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.company</groupId>
    <artifactId>my-module</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>common</module>
        <module>gateway</module>
        <module>designer</module>
        <module>client</module>
        <module>build</module>
    </modules>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <ignition.version>8.3.0</ignition.version>
        <module.name>My Custom Module</module.name>
        <module.description>Custom functionality for Ignition</module.description>
    </properties>

    <!-- Inductive Automation Repository -->
    <repositories>
        <repository>
            <id>ia-releases</id>
            <url>https://nexus.inductiveautomation.com/repository/inductiveautomation-releases/</url>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
    </repositories>

    <pluginRepositories>
        <pluginRepository>
            <id>ia-releases</id>
            <url>https://nexus.inductiveautomation.com/repository/inductiveautomation-releases/</url>
        </pluginRepository>
    </pluginRepositories>

    <!-- Dependency Management -->
    <dependencyManagement>
        <dependencies>
            <!-- Ignition SDK -->
            <dependency>
                <groupId>com.inductiveautomation.ignitionsdk</groupId>
                <artifactId>ignition-common</artifactId>
                <version>${ignition.version}</version>
                <scope>provided</scope>
            </dependency>
            
            <dependency>
                <groupId>com.inductiveautomation.ignitionsdk</groupId>
                <artifactId>gateway-api</artifactId>
                <version>${ignition.version}</version>
                <scope>provided</scope>
            </dependency>
            
            <!-- Testing -->
            <dependency>
                <groupId>org.junit.jupiter</groupId>
                <artifactId>junit-jupiter</artifactId>
                <version>5.9.0</version>
                <scope>test</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.10.1</version>
                    <configuration>
                        <source>17</source>
                        <target>17</target>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

### Build Module pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.company</groupId>
        <artifactId>my-module</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>my-module-build</artifactId>
    <packaging>pom</packaging>

    <dependencies>
        <!-- Include all submodules -->
        <dependency>
            <groupId>com.company</groupId>
            <artifactId>my-module-common</artifactId>
            <version>${project.version}</version>
            <scope>provided</scope>
        </dependency>
        
        <dependency>
            <groupId>com.company</groupId>
            <artifactId>my-module-gateway</artifactId>
            <version>${project.version}</version>
            <scope>provided</scope>
        </dependency>
        
        <dependency>
            <groupId>com.company</groupId>
            <artifactId>my-module-designer</artifactId>
            <version>${project.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Ignition Maven Plugin -->
            <plugin>
                <groupId>com.inductiveautomation.ignitionsdk</groupId>
                <artifactId>ignition-maven-plugin</artifactId>
                <version>1.2.2</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>modl</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <!-- Module ID -->
                    <moduleId>com.company.mymodule</moduleId>
                    
                    <!-- Module name and description -->
                    <moduleName>${module.name}</moduleName>
                    <moduleDescription>${module.description}</moduleDescription>
                    
                    <!-- Module version -->
                    <moduleVersion>${project.version}</moduleVersion>
                    
                    <!-- Required Ignition version -->
                    <requiredIgnitionVersion>8.3.0</requiredIgnitionVersion>
                    <requiredFrameworkVersion>8</requiredFrameworkVersion>
                    
                    <!-- License and docs -->
                    <license>license.html</license>
                    <documentationIndex>doc/index.html</documentationIndex>
                    
                    <!-- Hook classes -->
                    <hooks>
                        <hook>
                            <scope>G</scope>
                            <hookClass>com.company.mymodule.gateway.GatewayHook</hookClass>
                        </hook>
                        <hook>
                            <scope>D</scope>
                            <hookClass>com.company.mymodule.designer.DesignerHook</hookClass>
                        </hook>
                    </hooks>
                    
                    <!-- Module scopes -->
                    <projectScopes>
                        <projectScope>
                            <name>my-module-gateway</name>
                            <scope>G</scope>
                        </projectScope>
                        <projectScope>
                            <name>my-module-designer</name>
                            <scope>D</scope>
                        </projectScope>
                        <projectScope>
                            <name>my-module-common</name>
                            <scope>GD</scope>
                        </projectScope>
                    </projectScopes>
                    
                    <!-- Module dependencies -->
                    <moduleDependencies>
                        <depends>
                            <scope>D</scope>
                            <moduleId>com.inductiveautomation.vision</moduleId>
                        </depends>
                    </moduleDependencies>
                    
                    <!-- Skip signing for development -->
                    <skipModlSigning>true</skipModlSigning>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### Submodule pom.xml Example (Gateway)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.company</groupId>
        <artifactId>my-module</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>my-module-gateway</artifactId>

    <dependencies>
        <!-- Common submodule -->
        <dependency>
            <groupId>com.company</groupId>
            <artifactId>my-module-common</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <!-- Gateway SDK -->
        <dependency>
            <groupId>com.inductiveautomation.ignitionsdk</groupId>
            <artifactId>gateway-api</artifactId>
            <scope>provided</scope>
        </dependency>
        
        <!-- Third-party libraries -->
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.8.9</version>
        </dependency>
    </dependencies>
</project>
```

### Common Maven Commands

```bash
# Build the module
mvn clean package

# Build without tests
mvn clean package -DskipTests

# Run tests only
mvn test

# Install to local Maven repository
mvn install

# Clean build artifacts
mvn clean

# Deploy module to Gateway (with post goal)
mvn clean package ignition:post

# Print dependency tree
mvn dependency:tree
```

### Maven Tips

#### Custom Profiles

```xml
<profiles>
    <profile>
        <id>development</id>
        <properties>
            <skipModlSigning>true</skipModlSigning>
        </properties>
    </profile>
    
    <profile>
        <id>production</id>
        <properties>
            <skipModlSigning>false</skipModlSigning>
        </properties>
    </profile>
</profiles>
```

Use with: `mvn clean package -Pproduction`

## Creating New Module from Template

### Gradle

```bash
# Clone module tools
git clone https://github.com/inductiveautomation/ignition-module-tools.git
cd ignition-module-tools/generator

# Run generator
./gradlew run

# Follow interactive prompts:
# - Module name
# - Package name
# - Scopes needed
# - etc.
```

### Maven Archetype

```bash
mvn archetype:generate \
  -DarchetypeGroupId=com.inductiveautomation.ignitionsdk \
  -DarchetypeArtifactId=client-designer-gateway-archetype \
  -DarchetypeVersion=1.2.0 \
  -DgroupId=com.company \
  -DartifactId=my-module \
  -Dversion=1.0.0-SNAPSHOT \
  -Dpackage=com.company.mymodule
```

## Build Best Practices

### ✅ Do
- Use wrapper scripts (gradlew/mvnw) for consistent builds
- Version dependencies explicitly
- Keep build logic in build files, not hooks
- Use dependency management (Gradle: versions plugin, Maven: dependencyManagement)
- Test builds on clean environments
- Document custom build steps
- Use semantic versioning

### ❌ Don't
- Commit built artifacts to version control
- Use SNAPSHOT dependencies in production
- Ignore dependency conflicts
- Mix Gradle and Maven in same project
- Hardcode absolute paths
- Skip tests in CI/CD

## IDE Integration

### IntelliJ IDEA
- Open project: File → Open → select build.gradle.kts or pom.xml
- Auto-imports dependencies
- Run/debug configurations auto-generated
- Built-in Gradle/Maven tools window

### Eclipse
- Import project: File → Import → Maven/Gradle Project
- Install Buildship (Gradle) or M2E (Maven) plugins
- Auto-imports dependencies

### VS Code
- Install "Java Extension Pack"
- Install "Gradle for Java" or "Maven for Java"
- Open project folder
- Extensions handle dependencies

## Troubleshooting

### Gradle Issues
```bash
# Clear Gradle cache
rm -rf ~/.gradle/caches/

# Refresh dependencies
./gradlew build --refresh-dependencies

# Debug build
./gradlew build --stacktrace --info
```

### Maven Issues
```bash
# Clear local repository
rm -rf ~/.m2/repository/

# Force update
mvn clean package -U

# Debug build
mvn clean package -X
```

### Common Problems

**Dependencies not found**:
- Check repository URLs
- Verify artifact versions exist
- Check network/proxy settings

**Build fails**:
- Check Java version
- Review error messages
- Verify all subprojects compile

**Module won't load**:
- Check module.xml generated correctly
- Verify all hook classes exist
- Check for dependency conflicts
