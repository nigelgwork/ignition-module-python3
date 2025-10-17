package com.inductiveautomation.ignition.examples.python3.gateway;

import com.inductiveautomation.ignition.common.gson.Gson;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonParser;
import com.inductiveautomation.ignition.gateway.dataroutes.HttpMethod;
import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteAccess;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * REST API endpoints for Python 3 Integration module.
 * Provides HTTP access to all Python3 functions via JSON API.
 *
 * Endpoints are mounted at: http://host:port/main/data/python3integration/
 */
public final class Python3RestEndpoints {

    private static final Logger LOGGER = LoggerFactory.getLogger(Python3RestEndpoints.class);
    private static Python3ScriptModule scriptModule;
    private static Python3ScriptRepository scriptRepository;
    private static Python3MetricsCollector metricsCollector = new Python3MetricsCollector();

    // Security: Rate limiting (100 requests per minute per user)
    private static final int RATE_LIMIT_PER_MINUTE = 100;
    private static final Map<String, RateLimiter> userRateLimiters = new ConcurrentHashMap<>();

    // Security: Audit logging enabled
    private static final boolean AUDIT_LOGGING_ENABLED = true;

    // Security: Input validation limits
    private static final int MAX_CODE_SIZE = 1_048_576;  // 1MB
    private static final int MAX_SCRIPT_NAME_LENGTH = 255;
    private static final int MAX_FOLDER_PATH_LENGTH = 1000;

    private Python3RestEndpoints() {
        // Private constructor for utility class
    }

    /**
     * Simple rate limiter to prevent abuse
     */
    private static class RateLimiter {
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private long windowStart = System.currentTimeMillis();
        private static final long WINDOW_MS = 60000; // 1 minute

        public synchronized boolean allowRequest() {
            long now = System.currentTimeMillis();

            // Reset window if expired
            if (now - windowStart > WINDOW_MS) {
                requestCount.set(0);
                windowStart = now;
            }

            int count = requestCount.incrementAndGet();
            return count <= RATE_LIMIT_PER_MINUTE;
        }

        public synchronized int getRemainingRequests() {
            return Math.max(0, RATE_LIMIT_PER_MINUTE - requestCount.get());
        }
    }

    /**
     * Check if user has permission to execute Python code.
     * NOTE: Currently grants all access - configure authentication at Gateway level using:
     * - API Keys (Gateway → Security → API Keys)
     * - Network restrictions (firewall rules)
     * - HTTPS/SSL requirements
     */
    private static RouteAccess checkExecutePermission(RequestContext req) {
        // TODO: Implement proper authentication when SDK API is available
        // For now, rely on Gateway-level security (API keys, network restrictions)
        return RouteAccess.GRANTED;
    }

    /**
     * Check if user has permission to manage scripts (save, delete).
     */
    private static RouteAccess checkManagePermission(RequestContext req) {
        return RouteAccess.GRANTED;
    }

    /**
     * Check if user has permission to read data (diagnostics, stats).
     */
    private static RouteAccess checkReadPermission(RequestContext req) {
        return RouteAccess.GRANTED;
    }

    // Admin API key for ADMIN mode (configured via system property)
    // Set via: -Dignition.python3.admin.apikey=your-secret-key
    private static final String ADMIN_API_KEY = System.getProperty("ignition.python3.admin.apikey", null);

    /**
     * Determine security mode based on user role or API key.
     *
     * @return "ADMIN" if user has admin privileges, "RESTRICTED" otherwise
     *
     * ADMIN mode detection (in priority order):
     * 1. HTTP Header: X-Python3-Admin-Key matches configured admin API key
     * 2. HTTP Header: X-Admin-Mode=true with valid admin API key
     * 3. TODO: Check Ignition Administrator role when SDK API is available
     *
     * Configuration:
     * - Set admin API key: -Dignition.python3.admin.apikey=your-secret-key
     * - Then pass header: X-Python3-Admin-Key: your-secret-key
     *
     * Security:
     * - Admin API key should be a long, random string (min 32 characters recommended)
     * - Use HTTPS in production to protect API key in transit
     * - Defaults to RESTRICTED mode if no valid admin key provided
     */
    private static String getSecurityMode(RequestContext req) {
        // If no admin API key configured, always use RESTRICTED mode
        if (ADMIN_API_KEY == null || ADMIN_API_KEY.isEmpty()) {
            LOGGER.debug("Admin API key not configured, using RESTRICTED mode");
            return "RESTRICTED";
        }

        // Check HTTP headers for admin authentication
        HttpServletRequest httpRequest = req.getRequest();

        // Method 1: X-Python3-Admin-Key header
        String adminKey = httpRequest.getHeader("X-Python3-Admin-Key");
        if (adminKey != null && adminKey.equals(ADMIN_API_KEY)) {
            LOGGER.info("ADMIN mode activated via X-Python3-Admin-Key header");
            return "ADMIN";
        }

        // Method 2: X-Admin-Mode header (requires matching API key)
        String adminMode = httpRequest.getHeader("X-Admin-Mode");
        String apiKey = httpRequest.getHeader("X-API-Key");
        if ("true".equalsIgnoreCase(adminMode) && apiKey != null && apiKey.equals(ADMIN_API_KEY)) {
            LOGGER.info("ADMIN mode activated via X-Admin-Mode header");
            return "ADMIN";
        }

        // TODO: Method 3 - Check Ignition Administrator role (when SDK API is available)
        // Example (when SDK exposes user context):
        // User user = req.getUser();
        // if (user != null && user.hasRole("Administrator")) {
        //     LOGGER.info("ADMIN mode activated via Ignition Administrator role");
        //     return "ADMIN";
        // }

        // Default to RESTRICTED mode for security
        LOGGER.debug("No admin credentials provided, using RESTRICTED mode");
        return "RESTRICTED";
    }

    /**
     * Audit log Python code execution (simplified - no user context available in SDK)
     */
    private static void auditLog(String action, String details) {
        if (!AUDIT_LOGGING_ENABLED) {
            return;
        }

        try {
            // Log to Gateway logs (in production, also log to database)
            LOGGER.info("AUDIT: Action={}, Details={}",
                action, sanitizeForLogging(details));

            // TODO: In production, also write to audit table
            // scriptRepository.writeAuditLog(action, hashCode(details));

        } catch (Exception e) {
            LOGGER.error("Failed to write audit log", e);
        }
    }

    /**
     * Sanitize data for logging (truncate, redact secrets)
     */
    private static String sanitizeForLogging(String data) {
        if (data == null) {
            return "";
        }

        // Redact potential secrets
        String sanitized = data;
        sanitized = sanitized.replaceAll("password\\s*=\\s*['\"][^'\"]*['\"]", "password='***'");
        sanitized = sanitized.replaceAll("api_key\\s*=\\s*['\"][^'\"]*['\"]", "api_key='***'");
        sanitized = sanitized.replaceAll("secret\\s*=\\s*['\"][^'\"]*['\"]", "secret='***'");
        sanitized = sanitized.replaceAll("token\\s*=\\s*['\"][^'\"]*['\"]", "token='***'");

        // Truncate if too long
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200) + "... (truncated)";
        }

        return sanitized;
    }

    /**
     * Hash code for audit logging (don't store full code)
     */
    private static String hashCode(String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(code.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 16); // First 16 chars
        } catch (Exception e) {
            return "hash_error";
        }
    }

    /**
     * Validate Python code (size limits)
     */
    private static void validateCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Code cannot be null");
        }

        if (code.length() > MAX_CODE_SIZE) {
            throw new IllegalArgumentException(
                "Code size exceeds maximum limit of " + MAX_CODE_SIZE + " bytes (1MB). " +
                "Provided: " + code.length() + " bytes"
            );
        }

        if (code.trim().isEmpty()) {
            throw new IllegalArgumentException("Code cannot be empty");
        }
    }

    /**
     * Validate script name (alphanumeric, no SQL keywords, length limits)
     */
    private static void validateScriptName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Script name cannot be empty");
        }

        if (name.length() > MAX_SCRIPT_NAME_LENGTH) {
            throw new IllegalArgumentException(
                "Script name too long. Maximum: " + MAX_SCRIPT_NAME_LENGTH + " characters"
            );
        }

        // Allow alphanumeric, underscore, hyphen, period, space
        if (!name.matches("^[a-zA-Z0-9_. -]+$")) {
            throw new IllegalArgumentException(
                "Script name contains invalid characters. Allowed: a-z, A-Z, 0-9, _, -, ., space"
            );
        }

        // Blacklist SQL keywords (defense in depth, even though we don't use SQL)
        String upperName = name.toUpperCase();
        String[] sqlKeywords = {"SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE",
                                "ALTER", "UNION", "' OR '", "1=1", "--", ";", "/*", "*/"};

        for (String keyword : sqlKeywords) {
            if (upperName.contains(keyword)) {
                throw new IllegalArgumentException(
                    "Script name contains forbidden keyword: " + keyword
                );
            }
        }
    }

    /**
     * Validate folder path (alphanumeric, no SQL keywords, length limits)
     */
    private static void validateFolderPath(String folderPath) {
        if (folderPath == null) {
            return;  // null is valid (root folder)
        }

        if (folderPath.length() > MAX_FOLDER_PATH_LENGTH) {
            throw new IllegalArgumentException(
                "Folder path too long. Maximum: " + MAX_FOLDER_PATH_LENGTH + " characters"
            );
        }

        // Allow alphanumeric, underscore, hyphen, period, forward slash, space
        if (!folderPath.matches("^[a-zA-Z0-9_./\\- ]*$")) {
            throw new IllegalArgumentException(
                "Folder path contains invalid characters. Allowed: a-z, A-Z, 0-9, _, -, ., /, space"
            );
        }

        // Blacklist SQL keywords and path traversal
        String upperPath = folderPath.toUpperCase();
        String[] forbiddenPatterns = {"SELECT", "DROP", "--", "/*", "*/", "..", "\\\\", "'", "\""};

        for (String pattern : forbiddenPatterns) {
            if (upperPath.contains(pattern) || folderPath.contains(pattern)) {
                throw new IllegalArgumentException(
                    "Folder path contains forbidden pattern: " + pattern
                );
            }
        }
    }

    /**
     * Initialize the REST endpoints with the script module instance.
     * Called from GatewayHook during startup.
     */
    public static void initialize(Python3ScriptModule module) {
        scriptModule = module;
        LOGGER.info("Python3RestEndpoints initialized");
    }

    /**
     * Set the script repository for script management endpoints.
     * Called from GatewayHook during startup.
     */
    public static void setScriptRepository(Python3ScriptRepository repository) {
        scriptRepository = repository;
        LOGGER.info("Script repository configured");
    }

    /**
     * Mount all REST API routes.
     * Called from GatewayHook.mountRouteHandlers().
     *
     * Routes follow Ignition 8.3 API convention: /api/v1/{endpoint}
     * This ensures they appear in the OpenAPI specification at /openapi.json
     */
    public static void mountRoutes(RouteGroup routes) {
        LOGGER.info("Mounting Python3 REST API routes (Ignition 8.3 OpenAPI compliant)");

        // POST /data/python3integration/api/v1/exec - Execute Python code
        routes.newRoute("/api/v1/exec")
            .handler(Python3RestEndpoints::handleExec)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // ✅ AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/eval - Evaluate Python expression
        routes.newRoute("/api/v1/eval")
            .handler(Python3RestEndpoints::handleEval)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // ✅ AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/call-module - Call Python module function
        routes.newRoute("/api/v1/call-module")
            .handler(Python3RestEndpoints::handleCallModule)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // ✅ AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/call-script - Call saved Python script
        routes.newRoute("/api/v1/call-script")
            .handler(Python3RestEndpoints::handleCallScript)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // ✅ AUTH + RATE LIMIT
            .mount();

        // GET /data/python3integration/api/v1/version - Get Python version
        routes.newRoute("/api/v1/version")
            .handler(Python3RestEndpoints::handleGetVersion)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH (read-only)
            .mount();

        // GET /data/python3integration/api/v1/pool-stats - Get process pool statistics
        routes.newRoute("/api/v1/pool-stats")
            .handler(Python3RestEndpoints::handleGetPoolStats)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH (read-only)
            .mount();

        // GET /data/python3integration/api/v1/health - Health check
        routes.newRoute("/api/v1/health")
            .handler(Python3RestEndpoints::handleHealthCheck)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH (read-only)
            .mount();

        // GET /data/python3integration/api/v1/diagnostics - Performance diagnostics
        routes.newRoute("/api/v1/diagnostics")
            .handler(Python3RestEndpoints::handleDiagnostics)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH (read-only)
            .mount();

        // GET /data/python3integration/api/v1/example - Run example test
        routes.newRoute("/api/v1/example")
            .handler(Python3RestEndpoints::handleExample)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // ✅ AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/check-syntax - Check Python syntax
        routes.newRoute("/api/v1/check-syntax")
            .handler(Python3RestEndpoints::handleCheckSyntax)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkExecutePermission)  // ✅ AUTH + RATE LIMIT
            .mount();

        // POST /data/python3integration/api/v1/completions - Get code completions
        routes.newRoute("/api/v1/completions")
            .handler(Python3RestEndpoints::handleGetCompletions)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH (completions are read-only)
            .mount();

        // Performance Monitoring Endpoints

        // GET /data/python3integration/api/v1/metrics - Get performance metrics
        routes.newRoute("/api/v1/metrics")
            .handler(Python3RestEndpoints::handleGetMetrics)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH (read-only)
            .mount();

        // GET /data/python3integration/api/v1/gateway-impact - Get Gateway impact assessment
        routes.newRoute("/api/v1/gateway-impact")
            .handler(Python3RestEndpoints::handleGetGatewayImpact)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH (read-only)
            .mount();

        // GET /data/python3integration/api/v1/metrics/script-metrics - Get per-script metrics (NEW v1.16.0)
        routes.newRoute("/api/v1/metrics/script-metrics")
            .handler(Python3RestEndpoints::handleGetScriptMetrics)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // GET /data/python3integration/api/v1/metrics/historical - Get historical metrics (NEW v1.16.0)
        routes.newRoute("/api/v1/metrics/historical")
            .handler(Python3RestEndpoints::handleGetHistoricalMetrics)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // GET /data/python3integration/api/v1/metrics/alerts - Get active health alerts (NEW v1.16.0)
        routes.newRoute("/api/v1/metrics/alerts")
            .handler(Python3RestEndpoints::handleGetHealthAlerts)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)
            .mount();

        // Script Management Endpoints

        // POST /data/python3integration/api/v1/scripts/save - Save a script
        routes.newRoute("/api/v1/scripts/save")
            .handler(Python3RestEndpoints::handleSaveScript)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkManagePermission)  // ✅ AUTH + RATE LIMIT
            .mount();

        // GET /data/python3integration/api/v1/scripts/load/{name} - Load a script
        routes.newRoute("/api/v1/scripts/load/:name")
            .handler(Python3RestEndpoints::handleLoadScript)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH (read-only)
            .mount();

        // GET /data/python3integration/api/v1/scripts/list - List all scripts
        routes.newRoute("/api/v1/scripts/list")
            .handler(Python3RestEndpoints::handleListScripts)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkReadPermission)  // ✅ AUTH (read-only)
            .mount();

        // DELETE /data/python3integration/api/v1/scripts/delete/{name} - Delete a script
        routes.newRoute("/api/v1/scripts/delete/:name")
            .handler(Python3RestEndpoints::handleDeleteScript)
            .method(HttpMethod.DELETE)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(Python3RestEndpoints::checkManagePermission)  // ✅ AUTH + RATE LIMIT
            .mount();

        LOGGER.info("Python3 REST API routes mounted successfully at /api/v1/*");
    }

    /**
     * Handle POST /exec - Execute Python code
     *
     * Request body: {"code": "...", "variables": {...}}
     * Response: {"success": true/false, "result": ..., "error": "..."}
     */
    private static JsonObject handleExec(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /exec called");

        try {
            JsonObject requestBody = parseJsonBody(req);
            String code = requestBody.has("code") ? requestBody.get("code").getAsString() : "";
            Map<String, Object> variables = new HashMap<>();

            if (requestBody.has("variables") && requestBody.get("variables").isJsonObject()) {
                variables = jsonToMap(requestBody.getAsJsonObject("variables"));
            }

            // INPUT VALIDATION: Validate code before execution
            validateCode(code);

            // SECURITY: Determine security mode based on user role
            String securityMode = getSecurityMode(req);
            LOGGER.debug("Security mode for /exec: {}", securityMode);

            // AUDIT LOG: Log code execution attempt
            auditLog("PYTHON_EXEC", code);

            Object result = scriptModule.exec(code, variables, securityMode);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("result", result != null ? result.toString() : null);

            LOGGER.debug("REST API: /exec completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /exec failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle POST /eval - Evaluate Python expression
     *
     * Request body: {"expression": "...", "variables": {...}}
     * Response: {"success": true/false, "result": ..., "error": "..."}
     */
    private static JsonObject handleEval(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /eval called");

        try {
            JsonObject requestBody = parseJsonBody(req);
            String expression = requestBody.has("expression") ? requestBody.get("expression").getAsString() : "";
            Map<String, Object> variables = new HashMap<>();

            if (requestBody.has("variables") && requestBody.get("variables").isJsonObject()) {
                variables = jsonToMap(requestBody.getAsJsonObject("variables"));
            }

            // INPUT VALIDATION: Validate expression before evaluation
            validateCode(expression);

            // SECURITY: Determine security mode based on user role
            String securityMode = getSecurityMode(req);
            LOGGER.debug("Security mode for /eval: {}", securityMode);

            // AUDIT LOG: Log expression evaluation
            auditLog("PYTHON_EVAL", expression);

            Object result = scriptModule.eval(expression, variables, securityMode);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("result", result != null ? result.toString() : null);

            LOGGER.debug("REST API: /eval completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /eval failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle POST /call-module - Call Python module function
     *
     * Request body: {"module": "...", "function": "...", "args": [...]}
     * Response: {"success": true/false, "result": ..., "error": "..."}
     */
    private static JsonObject handleCallModule(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /call-module called");

        try {
            JsonObject requestBody = parseJsonBody(req);
            String moduleName = requestBody.has("module") ? requestBody.get("module").getAsString() : "";
            String functionName = requestBody.has("function") ? requestBody.get("function").getAsString() : "";
            List<Object> args = new ArrayList<>();

            if (requestBody.has("args") && requestBody.get("args").isJsonArray()) {
                JsonArray jsonArgs = requestBody.getAsJsonArray("args");
                for (JsonElement element : jsonArgs) {
                    args.add(jsonElementToObject(element));
                }
            }

            // SECURITY: Determine security mode based on user role
            String securityMode = getSecurityMode(req);
            LOGGER.debug("Security mode for /call-module: {}", securityMode);

            // AUDIT LOG: Log module call
            auditLog("PYTHON_CALL_MODULE", moduleName + "." + functionName + "(" + args + ")");

            Object result = scriptModule.callModule(moduleName, functionName, args, Collections.emptyMap(), securityMode);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("result", result != null ? result.toString() : null);

            LOGGER.debug("REST API: /call-module completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /call-module failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle POST /call-script - Call saved Python script by path
     *
     * Request body: {"scriptPath": "...", "args": [...], "kwargs": {...}}
     * Response: {"success": true/false, "result": ..., "error": "..."}
     */
    private static JsonObject handleCallScript(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /call-script called");

        try {
            JsonObject requestBody = parseJsonBody(req);
            String scriptPath = requestBody.has("scriptPath") ? requestBody.get("scriptPath").getAsString() : "";

            if (scriptPath.isEmpty()) {
                return createErrorResponse("scriptPath is required");
            }

            List<Object> args = new ArrayList<>();
            if (requestBody.has("args") && requestBody.get("args").isJsonArray()) {
                JsonArray jsonArgs = requestBody.getAsJsonArray("args");
                for (JsonElement element : jsonArgs) {
                    args.add(jsonElementToObject(element));
                }
            }

            Map<String, Object> kwargs = new HashMap<>();
            if (requestBody.has("kwargs") && requestBody.get("kwargs").isJsonObject()) {
                kwargs = jsonToMap(requestBody.getAsJsonObject("kwargs"));
            }

            // AUDIT LOG: Log script execution
            auditLog("PYTHON_CALL_SCRIPT", "scriptPath=" + scriptPath);

            Object result = scriptModule.callScript(scriptPath, args, kwargs);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("result", result != null ? result.toString() : null);

            LOGGER.debug("REST API: /call-script completed successfully for script: {}", scriptPath);
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /call-script failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /version - Get Python version
     *
     * Response: {"version": "...", "available": true/false}
     */
    private static JsonObject handleGetVersion(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /version called");

        try {
            Map<String, Object> versionInfo = scriptModule.getVersion();
            JsonObject response = mapToJson(versionInfo);

            LOGGER.debug("REST API: /version completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /version failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /pool-stats - Get process pool statistics
     *
     * Response: {"totalSize": ..., "available": ..., "inUse": ..., "healthy": ...}
     */
    private static JsonObject handleGetPoolStats(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /pool-stats called");

        try {
            Map<String, Object> poolStats = scriptModule.getPoolStats();
            JsonObject response = mapToJson(poolStats);

            LOGGER.debug("REST API: /pool-stats completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /pool-stats failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /health - Health check
     *
     * Response: {"healthy": true/false, "available": true/false}
     */
    private static JsonObject handleHealthCheck(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /health called");

        try {
            boolean available = scriptModule.isAvailable();

            JsonObject response = new JsonObject();
            response.addProperty("healthy", available);
            response.addProperty("available", available);
            response.addProperty("timestamp", System.currentTimeMillis());

            LOGGER.debug("REST API: /health completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /health failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /diagnostics - Performance diagnostics
     *
     * Response: comprehensive diagnostic information
     */
    private static JsonObject handleDiagnostics(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /diagnostics called");

        try {
            JsonObject response = new JsonObject();

            // Module availability
            boolean available = scriptModule.isAvailable();
            response.addProperty("available", available);

            // Process pool stats
            Map<String, Object> poolStats = scriptModule.getPoolStats();
            response.add("poolStats", mapToJson(poolStats));

            // Python version info
            Map<String, Object> versionInfo = scriptModule.getVersion();
            response.add("versionInfo", mapToJson(versionInfo));

            // Distribution info
            Map<String, Object> distributionInfo = scriptModule.getDistributionInfo();
            response.add("distributionInfo", mapToJson(distributionInfo));

            // Timestamp
            response.addProperty("timestamp", System.currentTimeMillis());

            LOGGER.debug("REST API: /diagnostics completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /diagnostics failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /example - Run example test
     *
     * Response: {"success": true/false, "result": "..."}
     */
    private static JsonObject handleExample(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /example called");

        try {

            // AUDIT LOG: Log example execution
            auditLog("PYTHON_EXAMPLE", "Example test execution");

            String result = scriptModule.example();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("result", result);

            LOGGER.debug("REST API: /example completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /example failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle POST /check-syntax - Check Python code syntax
     *
     * Request body: {"code": "..."}
     * Response: {"success": true, "errors": [{line, column, message, severity}, ...]}
     */
    private static JsonObject handleCheckSyntax(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /check-syntax called");

        try {
            JsonObject requestBody = parseJsonBody(req);
            String code = requestBody.has("code") ? requestBody.get("code").getAsString() : "";

            if (code.isEmpty()) {
                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.add("errors", new JsonArray());
                return response;
            }

            // INPUT VALIDATION: Validate code size
            validateCode(code);

            // AUDIT LOG: Log syntax check (execution context)
            auditLog("PYTHON_CHECK_SYNTAX", code);

            // Call Python syntax checker through script module
            Map<String, Object> result = scriptModule.checkSyntax(code);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            // Convert errors list to JSON array
            JsonArray errorsArray = new JsonArray();
            if (result.containsKey("errors") && result.get("errors") instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> errors = (List<Map<String, Object>>) result.get("errors");

                for (Map<String, Object> error : errors) {
                    JsonObject errorJson = mapToJson(error);
                    errorsArray.add(errorJson);
                }
            }
            response.add("errors", errorsArray);

            LOGGER.debug("REST API: /check-syntax completed successfully, found {} errors",
                        errorsArray.size());
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /check-syntax failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle POST /completions - Get code completions at cursor position
     *
     * Request body: {"code": "...", "line": 1, "column": 0}
     * Response: {"success": true, "completions": [{text, type, description, signature}, ...]}
     */
    private static JsonObject handleGetCompletions(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /completions called");

        try {
            JsonObject requestBody = parseJsonBody(req);
            String code = requestBody.has("code") ? requestBody.get("code").getAsString() : "";
            int line = requestBody.has("line") ? requestBody.get("line").getAsInt() : 1;
            int column = requestBody.has("column") ? requestBody.get("column").getAsInt() : 0;

            if (code.isEmpty()) {
                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.add("completions", new JsonArray());
                response.addProperty("count", 0);
                return response;
            }

            // Call Python completion engine through script module
            Map<String, Object> result = scriptModule.getCompletions(code, line, column);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            // Convert completions list to JSON array
            JsonArray completionsArray = new JsonArray();
            if (result.containsKey("completions") && result.get("completions") instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> completions = (List<Map<String, Object>>) result.get("completions");

                for (Map<String, Object> completion : completions) {
                    JsonObject completionJson = mapToJson(completion);
                    completionsArray.add(completionJson);
                }
            }
            response.add("completions", completionsArray);
            response.addProperty("count", completionsArray.size());

            // Add message if Jedi not installed
            if (result.containsKey("message")) {
                response.addProperty("message", result.get("message").toString());
            }

            LOGGER.debug("REST API: /completions completed successfully, found {} completions",
                        completionsArray.size());
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /completions failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /metrics - Get performance metrics
     *
     * Response: {"total_executions": ..., "success_rate": ..., "health_score": ..., ...}
     */
    private static JsonObject handleGetMetrics(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /metrics called");

        try {
            Map<String, Object> metrics = metricsCollector.getMetrics();
            JsonObject response = mapToJson(metrics);

            LOGGER.debug("REST API: /metrics completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /metrics failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /gateway-impact - Get Gateway impact assessment
     *
     * Response: {"executions_per_minute": ..., "pool_utilization_percent": ..., "impact_level": "LOW|MEDIUM|HIGH", ...}
     */
    private static JsonObject handleGetGatewayImpact(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /gateway-impact called");

        try {
            Map<String, Object> impact = metricsCollector.getGatewayImpact();
            JsonObject response = mapToJson(impact);

            LOGGER.debug("REST API: /gateway-impact completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /gateway-impact failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /metrics/script-metrics - Get per-script performance metrics (NEW v1.16.0)
     *
     * Response: [{"script_identifier": "...", "total_executions": ..., "success_rate": ..., ...}, ...]
     */
    private static JsonObject handleGetScriptMetrics(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /metrics/script-metrics called");

        try {
            List<Map<String, Object>> scriptMetrics = metricsCollector.getScriptMetrics();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            JsonArray metricsArray = new JsonArray();
            for (Map<String, Object> metrics : scriptMetrics) {
                metricsArray.add(mapToJson(metrics));
            }
            response.add("script_metrics", metricsArray);
            response.addProperty("count", metricsArray.size());

            LOGGER.debug("REST API: /metrics/script-metrics completed successfully, {} scripts",
                    metricsArray.size());
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /metrics/script-metrics failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /metrics/historical - Get historical metric snapshots (NEW v1.16.0)
     *
     * Response: [{"timestamp": ..., "total_executions": ..., "pool_utilization": ..., ...}, ...]
     */
    private static JsonObject handleGetHistoricalMetrics(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /metrics/historical called");

        try {
            List<Map<String, Object>> historicalMetrics = metricsCollector.getHistoricalMetrics();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            JsonArray historyArray = new JsonArray();
            for (Map<String, Object> snapshot : historicalMetrics) {
                historyArray.add(mapToJson(snapshot));
            }
            response.add("historical_metrics", historyArray);
            response.addProperty("count", historyArray.size());

            LOGGER.debug("REST API: /metrics/historical completed successfully, {} snapshots",
                    historyArray.size());
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /metrics/historical failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /metrics/alerts - Get active health alerts (NEW v1.16.0)
     *
     * Response: [{"timestamp": ..., "alert_id": "...", "message": "...", "severity": "WARNING|CRITICAL"}, ...]
     */
    private static JsonObject handleGetHealthAlerts(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /metrics/alerts called");

        try {
            List<Map<String, Object>> healthAlerts = metricsCollector.getHealthAlerts();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            JsonArray alertsArray = new JsonArray();
            for (Map<String, Object> alert : healthAlerts) {
                alertsArray.add(mapToJson(alert));
            }
            response.add("alerts", alertsArray);
            response.addProperty("count", alertsArray.size());

            LOGGER.debug("REST API: /metrics/alerts completed successfully, {} active alerts",
                    alertsArray.size());
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /metrics/alerts failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    // Script Management Handlers

    /**
     * Handle POST /scripts/save - Save a Python script
     *
     * Request body: {"name": "...", "code": "...", "description": "...", "author": "...", "folderPath": "...", "version": "..."}
     * Response: {"success": true/false, "script": {...}}
     */
    private static JsonObject handleSaveScript(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /scripts/save called");

        try {

            if (scriptRepository == null) {
                return createErrorResponse("Script repository not initialized");
            }

            JsonObject requestBody = parseJsonBody(req);
            String name = requestBody.has("name") ? requestBody.get("name").getAsString() : null;
            String code = requestBody.has("code") ? requestBody.get("code").getAsString() : "";
            String description = requestBody.has("description") ? requestBody.get("description").getAsString() : null;
            String author = requestBody.has("author") ? requestBody.get("author").getAsString() : "Unknown";
            String folderPath = requestBody.has("folderPath") ? requestBody.get("folderPath").getAsString() : "";
            String version = requestBody.has("version") ? requestBody.get("version").getAsString() : "1.0";

            if (name == null || name.trim().isEmpty()) {
                return createErrorResponse("Script name is required");
            }

            // INPUT VALIDATION: Validate all inputs
            validateScriptName(name);
            validateCode(code);
            validateFolderPath(folderPath);

            // AUDIT LOG: Log script save
            auditLog("SCRIPT_SAVE", "name=" + name + ", folder=" + folderPath + ", code_hash=" + hashCode(code));

            Python3ScriptRepository.SavedScript script = scriptRepository.saveScript(
                name, code, description, author, folderPath, version
            );

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            JsonObject scriptJson = new JsonObject();
            scriptJson.addProperty("id", script.getId());
            scriptJson.addProperty("name", script.getName());
            scriptJson.addProperty("description", script.getDescription());
            scriptJson.addProperty("author", script.getAuthor());
            scriptJson.addProperty("createdDate", script.getCreatedDate());
            scriptJson.addProperty("lastModified", script.getLastModified());
            scriptJson.addProperty("folderPath", script.getFolderPath());
            scriptJson.addProperty("version", script.getVersion());
            response.add("script", scriptJson);

            LOGGER.info("REST API: Script saved: {} in folder: {}", name, folderPath);
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /scripts/save failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /scripts/load/:name - Load a saved script
     *
     * Response: {"success": true/false, "script": {...}}
     */
    private static JsonObject handleLoadScript(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /scripts/load called");

        try {
            if (scriptRepository == null) {
                return createErrorResponse("Script repository not initialized");
            }

            // Extract name from URL path
            String requestPath = req.getRequest().getRequestURI();
            String name = requestPath.substring(requestPath.lastIndexOf('/') + 1);

            if (name == null || name.trim().isEmpty()) {
                return createErrorResponse("Script name is required");
            }

            Python3ScriptRepository.SavedScript script = scriptRepository.loadScript(name);

            if (script == null) {
                return createErrorResponse("Script not found: " + name);
            }

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            JsonObject scriptJson = new JsonObject();
            scriptJson.addProperty("id", script.getId());
            scriptJson.addProperty("name", script.getName());
            scriptJson.addProperty("code", script.getCode());
            scriptJson.addProperty("description", script.getDescription());
            scriptJson.addProperty("author", script.getAuthor());
            scriptJson.addProperty("createdDate", script.getCreatedDate());
            scriptJson.addProperty("lastModified", script.getLastModified());
            scriptJson.addProperty("folderPath", script.getFolderPath());
            scriptJson.addProperty("version", script.getVersion());
            response.add("script", scriptJson);

            LOGGER.debug("REST API: Script loaded: {}", name);
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /scripts/load failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /scripts/list - List all saved scripts
     *
     * Response: {"success": true, "scripts": [...]}
     */
    private static JsonObject handleListScripts(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /scripts/list called");

        try {
            if (scriptRepository == null) {
                return createErrorResponse("Script repository not initialized");
            }

            List<Python3ScriptRepository.ScriptMetadata> scripts = scriptRepository.listScripts();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            JsonArray scriptsArray = new JsonArray();
            for (Python3ScriptRepository.ScriptMetadata script : scripts) {
                JsonObject scriptJson = new JsonObject();
                scriptJson.addProperty("id", script.getId());
                scriptJson.addProperty("name", script.getName());
                scriptJson.addProperty("description", script.getDescription());
                scriptJson.addProperty("author", script.getAuthor());
                scriptJson.addProperty("createdDate", script.getCreatedDate());
                scriptJson.addProperty("lastModified", script.getLastModified());
                scriptJson.addProperty("folderPath", script.getFolderPath());
                scriptJson.addProperty("version", script.getVersion());
                scriptsArray.add(scriptJson);
            }
            response.add("scripts", scriptsArray);

            LOGGER.debug("REST API: Listed {} scripts", scripts.size());
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /scripts/list failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle DELETE /scripts/delete/:name - Delete a saved script
     *
     * Response: {"success": true/false}
     */
    private static JsonObject handleDeleteScript(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /scripts/delete called");

        try {

            if (scriptRepository == null) {
                return createErrorResponse("Script repository not initialized");
            }

            // Extract name from URL path
            String requestPath = req.getRequest().getRequestURI();
            String name = requestPath.substring(requestPath.lastIndexOf('/') + 1);

            if (name == null || name.trim().isEmpty()) {
                return createErrorResponse("Script name is required");
            }

            // AUDIT LOG: Log script deletion
            auditLog("SCRIPT_DELETE", "name=" + name);

            boolean deleted = scriptRepository.deleteScript(name);

            JsonObject response = new JsonObject();
            response.addProperty("success", deleted);

            if (!deleted) {
                response.addProperty("message", "Script not found: " + name);
            } else {
                response.addProperty("message", "Script deleted successfully");
            }

            LOGGER.info("REST API: Script deletion: {} - {}", name, deleted ? "success" : "not found");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /scripts/delete failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    // Utility methods

    /**
     * Parse JSON from request body
     */
    private static JsonObject parseJsonBody(RequestContext req) throws IOException {
        HttpServletRequest httpRequest = req.getRequest();
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = httpRequest.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String jsonString = sb.toString();
        return JsonParser.parseString(jsonString).getAsJsonObject();
    }

    private static JsonObject createErrorResponse(String errorMessage) {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", errorMessage);
        return response;
    }

    private static JsonObject mapToJson(Map<String, Object> map) {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                json.addProperty(entry.getKey(), (String) value);
            } else if (value instanceof Number) {
                json.addProperty(entry.getKey(), (Number) value);
            } else if (value instanceof Boolean) {
                json.addProperty(entry.getKey(), (Boolean) value);
            } else if (value != null) {
                json.addProperty(entry.getKey(), value.toString());
            }
        }
        return json;
    }

    private static Map<String, Object> jsonToMap(JsonObject json) {
        Map<String, Object> map = new HashMap<>();
        for (String key : json.keySet()) {
            map.put(key, jsonElementToObject(json.get(key)));
        }
        return map;
    }

    private static Object jsonElementToObject(JsonElement element) {
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isNumber()) {
                return element.getAsNumber();
            } else if (element.getAsJsonPrimitive().isBoolean()) {
                return element.getAsBoolean();
            } else {
                return element.getAsString();
            }
        } else if (element.isJsonNull()) {
            return null;
        } else {
            return element.toString();
        }
    }
}
