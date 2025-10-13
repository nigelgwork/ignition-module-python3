# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Purpose

This is a **Python 3 Integration module** for Ignition 8.3 SDK. This is a standalone repository with all necessary SDK documentation, guides, and examples included.

## Repository Structure

```
ignition-module-python3/
├── CLAUDE.md                        # This file - AI guidance
├── README.md                        # Project overview
├── .gitignore                       # Git ignore rules
│
├── docs/
│   └── knowledge-base/              # Complete SDK documentation
│       ├── 00-CLAUDE-CODE-INSTRUCTIONS.md
│       ├── 01-SDK-Overview-Getting-Started.md
│       ├── 02-Module-Architecture-Structure.md
│       ├── 03-Build-Systems-Gradle-Maven.md
│       ├── 04-Perspective-Component-Development.md
│       ├── 05-Vision-Component-Development.md
│       ├── 06-OPC-UA-Device-Driver-Development.md
│       ├── 07-Scripting-Functions-RPC-Communication.md
│       └── 08-Quick-Reference-Cheat-Sheet.md
│
└── examples/                        # Official Ignition SDK examples
    └── (reference implementations)
```

## Knowledge Base

The complete Ignition SDK documentation is in `docs/knowledge-base/`:

1. **00-CLAUDE-CODE-INSTRUCTIONS.md** - Specific workflows for Claude Code
2. **01-SDK-Overview-Getting-Started.md** - SDK fundamentals, prerequisites
3. **02-Module-Architecture-Structure.md** - Module scopes, hooks, lifecycle
4. **03-Build-Systems-Gradle-Maven.md** - Build configuration
5. **04-Perspective-Component-Development.md** - React/TypeScript components
6. **05-Vision-Component-Development.md** - Java Swing components
7. **06-OPC-UA-Device-Driver-Development.md** - Device drivers
8. **07-Scripting-Functions-RPC-Communication.md** - Python functions, RPC
9. **08-Quick-Reference-Cheat-Sheet.md** - Quick lookup, snippets

## Python 3 Integration Goals

This module aims to provide Python 3 integration for Ignition. Potential goals:

- Python 3 runtime in Ignition
- Custom Python 3 scripting functions
- Gateway-side Python 3 scripts
- Designer integration for Python 3 tools
- Python package management

## Module Development Approach

### 1. Choose Module Type

For Python 3 integration, likely approaches:
- **Scripting Functions**: Add Python 3 functions to Ignition's scripting environment
- **Gateway Hooks**: Background Python 3 processes on Gateway
- **Designer Tools**: Python 3-based utilities in Designer

### 2. Review SDK Documentation

Always start with the knowledge base:
```bash
# Read in order
cat docs/knowledge-base/01-SDK-Overview-Getting-Started.md
cat docs/knowledge-base/02-Module-Architecture-Structure.md
cat docs/knowledge-base/03-Build-Systems-Gradle-Maven.md
```

### 3. Reference Official Examples

The `examples/` directory contains official Ignition SDK examples:
```bash
ls -la examples/
```

### 4. Create Module Structure

Use Gradle (recommended) or Maven:

```
python3-module/
├── build.gradle.kts
├── settings.gradle.kts
├── version.properties
├── common/
│   └── src/main/java/
├── gateway/
│   └── src/main/java/
└── designer/
    └── src/main/java/
```

## Core Concepts

### Module Scopes
- **Gateway (G)**: Server-side logic (Python runtime, background scripts)
- **Designer (D)**: Design-time tools (Python editors, utilities)
- **Client (C)**: Vision client runtime (usually not needed)
- **Common**: Shared code between scopes

### Module Lifecycle
Every module hook goes through three phases:
1. **setup()** - Early initialization, register extension points
2. **startup()** - Main initialization when platform services are available
3. **shutdown()** - Clean shutdown, stop threads, release resources

### Build Systems
Both Gradle (recommended) and Maven are supported:
- **Gradle**: `./gradlew build` → `build/*.modl`
- **Maven**: `mvn clean package` → `target/*.modl`

## Prerequisites

- Java JDK 17 (for Ignition 8.3)
- Ignition 8.3+ running locally (usually port 8088)
- Gradle or Maven
- Python 3.x (for integration)
- Enable unsigned modules: Add `-Dignition.allowunsignedmodules=true` to `ignition.conf`

## Official Resources

- **SDK Documentation**: https://www.sdk-docs.inductiveautomation.com/
- **Official Examples**: https://github.com/inductiveautomation/ignition-sdk-examples
- **Forum**: https://forum.inductiveautomation.com/c/module-development/7

## Critical Best Practices

### Always Do
- Use SLF4J logger (never System.out.println)
- Implement proper shutdown() to prevent memory leaks
- Handle exceptions gracefully
- Use thread-safe operations in Gateway scope
- Test module installation/uninstallation

### Never Do
- Block in setup() method
- Modify database in setup() (wait for startup())
- Forget to stop threads in shutdown()
- Use SNAPSHOT dependencies in production
- Hardcode configuration values

## Package Naming Convention

Use reverse domain notation:
- Module ID: `com.company.python3integration`
- Package structure: `com.company.python3integration.{scope}`
- Example: `com.company.python3integration.gateway.GatewayHook`

## Getting Started

1. **Define goals** - What Python 3 functionality do you want?
2. **Review docs** - Read SDK guides in `docs/knowledge-base/`
3. **Check examples** - Reference implementations in `examples/`
4. **Create structure** - Initialize Gradle project
5. **Implement hooks** - Gateway/Designer hooks with lifecycle
6. **Build and test** - Iterate on implementation

## Quick Commands

```bash
# Initialize Gradle project
gradle init --type java-library

# Build module (once created)
./gradlew clean build

# Check build output
ls -lh build/*.modl

# Install in Gateway
# Upload .modl file at http://localhost:8088
# Navigate to: Config → System → Modules
```

## Notes

This is a standalone repository with all necessary resources included:
- ✅ Complete SDK documentation (docs/knowledge-base/)
- ✅ Official SDK examples (examples/)
- ✅ Independent git repository ready
- ✅ No external dependencies on other module projects

Ready to start building Python 3 integration for Ignition! 🐍
