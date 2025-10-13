# Embedding Python in the Module

## Overview

To make the module truly self-contained, we can embed Python 3 distributions directly in the .modl file. This eliminates the need for users to install Python separately on their Gateway servers.

## Approaches

### Option 1: Python Standalone Builds (Recommended)

**What it is:**
- Pre-compiled, portable Python distributions
- From: https://github.com/indygreg/python-build-standalone
- No system dependencies required
- Works on Windows, Linux, macOS

**Pros:**
- ✅ Full CPython compatibility
- ✅ All packages work (numpy, pandas, etc.)
- ✅ No system dependencies
- ✅ Known to work reliably
- ✅ Multiple platform support

**Cons:**
- ❌ Large file size (~50-80MB per platform)
- ❌ Platform-specific (need separate builds)
- ❌ Still uses subprocess approach

**Implementation:**
```
.modl file structure:
├── python-distributions/
│   ├── python-windows-x86_64.tar.gz  (~60MB)
│   ├── python-linux-x86_64.tar.gz    (~50MB)
│   └── python-macos-x86_64.tar.gz    (~55MB)
└── ... (Java classes)

Total .modl size: ~200MB
```

### Option 2: GraalVM Python (Alternative)

**What it is:**
- Python implementation that runs on JVM
- Uses GraalVM Truffle framework
- No separate process needed

**Pros:**
- ✅ True Java integration
- ✅ No subprocess overhead
- ✅ Single platform (JVM)
- ✅ Smaller footprint (~30MB)

**Cons:**
- ❌ Not 100% CPython compatible
- ❌ Some packages don't work (C extensions need special handling)
- ❌ Slower for some operations
- ❌ Requires GraalVM dependencies

**Implementation:**
```gradle
dependencies {
    modlImplementation("org.graalvm.python:python:23.1.0")
    modlImplementation("org.graalvm.truffle:truffle-api:23.1.0")
}
```

### Option 3: Jython (Not Recommended)

**What it is:**
- Python 2.7 for JVM (Jython 2.7)
- Jython 3.x is not production-ready

**Pros:**
- ✅ Native JVM integration

**Cons:**
- ❌ Only Python 2.7 (defeats the purpose)
- ❌ Jython 3 is vaporware
- ❌ Limited package support

## Recommended Implementation: Embedded Standalone Python

### Architecture

```
Module Startup:
1. Detect OS (Windows/Linux/macOS)
2. Extract appropriate Python distribution from .modl resources
3. Extract to: <ignition-data>/modules/python3-integration/python/
4. Set Python path to extracted executable
5. Initialize process pool with embedded Python
```

### File Size Analysis

**Python Standalone Distributions:**
- Windows (x86_64): ~60MB compressed
- Linux (x86_64): ~50MB compressed
- macOS (x86_64 + arm64): ~55MB compressed
- **Total**: ~165MB

**With Common Packages (numpy, pandas, requests):**
- Additional ~50MB per platform
- **Total**: ~315MB

**Strategies to Reduce Size:**
1. **Platform detection**: Only bundle for detected platform (~60MB)
2. **Lazy download**: Download Python on first use
3. **External repository**: Host Python distributions separately
4. **Minimal Python**: Strip unused stdlib modules (~30% reduction)

### Implementation Steps

#### 1. Add Distribution Manager

```java
public class PythonDistributionManager {
    private final Path extractPath;
    private String pythonExecutable;

    public void extractPython() {
        // Detect OS
        String os = detectOS();

        // Extract from resources
        String resourcePath = "/python-distributions/python-" + os + ".tar.gz";
        extractTarGz(resourcePath, extractPath);

        // Set executable path
        pythonExecutable = extractPath.resolve("bin/python3").toString();
    }

    public String getPythonPath() {
        if (!isExtracted()) {
            extractPython();
        }
        return pythonExecutable;
    }
}
```

#### 2. Modify GatewayHook

```java
@Override
public void startup(LicenseState licenseState) {
    // Extract bundled Python
    PythonDistributionManager distManager = new PythonDistributionManager(
        gatewayContext.getSystemManager().getDataDir()
            .resolve("modules/python3-integration")
    );

    String pythonPath = distManager.getPythonPath();
    logger.info("Using embedded Python: {}", pythonPath);

    // Initialize pool with embedded Python
    processPool = new Python3ProcessPool(pythonPath, poolSize);
}
```

#### 3. Build Configuration

```gradle
// Download Python distributions at build time
task downloadPythonDistributions {
    doLast {
        def distributions = [
            'windows': 'https://github.com/indygreg/python-build-standalone/releases/download/20231002/cpython-3.11.6+20231002-x86_64-pc-windows-msvc-shared-install_only.tar.gz',
            'linux': 'https://github.com/indygreg/python-build-standalone/releases/download/20231002/cpython-3.11.6+20231002-x86_64-unknown-linux-gnu-install_only.tar.gz',
            'macos': 'https://github.com/indygreg/python-build-standalone/releases/download/20231002/cpython-3.11.6+20231002-x86_64-apple-darwin-install_only.tar.gz'
        ]

        distributions.each { os, url ->
            // Download and place in resources
        }
    }
}

// Include in module
modlImplementation(files('src/main/resources/python-distributions'))
```

## Practical Recommendation

### For Most Users: **Lazy Download Approach**

Best balance of convenience and file size:

```java
public class PythonDistributionManager {

    public String getPythonPath() throws IOException {
        // Check if already extracted
        if (isExtracted()) {
            return pythonExecutable;
        }

        // Try to use system Python first (if available)
        String systemPython = detectSystemPython();
        if (systemPython != null && isPythonValid(systemPython)) {
            logger.info("Using system Python: {}", systemPython);
            return systemPython;
        }

        // Download and extract bundled Python
        logger.info("Downloading embedded Python distribution...");
        downloadAndExtract();

        return pythonExecutable;
    }

    private void downloadAndExtract() {
        String os = detectOS();
        String url = DISTRIBUTION_URLS.get(os);

        // Download with progress
        Path downloadPath = Files.createTempFile("python", ".tar.gz");
        downloadWithProgress(url, downloadPath);

        // Extract
        extractTarGz(downloadPath, extractPath);

        logger.info("Python distribution installed successfully");
    }
}
```

**Benefits:**
- ✅ Small module size (~5MB without Python)
- ✅ Downloads Python automatically if needed
- ✅ Falls back to system Python if available
- ✅ One-time download (cached forever)
- ✅ No manual installation required

**User Experience:**
1. Install module (small file)
2. First use: "Downloading Python 3... (60MB)"
3. Subsequent uses: Instant (cached)

## Implementation Priority

### Phase 1: Lazy Download (Best Balance)
- Small module file
- Automatic Python acquisition
- Fallback to system Python
- **Time to implement: 1-2 days**

### Phase 2: Fully Embedded (Optional)
- For air-gapped environments
- Include distributions in .modl
- Larger file but no downloads
- **Time to implement: 1 day**

### Phase 3: GraalVM Option (Advanced)
- For maximum integration
- Trade compatibility for size
- **Time to implement: 3-5 days**

## Air-Gapped Environments

For environments without internet access:

### Option A: Bundle Everything
Build with all distributions included:
```bash
./gradlew build -Pinclude-python=true
# Creates: python3-integration-1.0.0-SNAPSHOT-full.modl (~200MB)
```

### Option B: Manual Installation
Provide installation script:
```bash
# On Gateway server
cd <ignition-data>/modules/python3-integration
./install-python.sh
# Extracts Python from provided tar.gz
```

## Sample Implementation

Let me create a complete implementation using the **Lazy Download** approach...
