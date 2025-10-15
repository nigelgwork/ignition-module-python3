package com.inductiveautomation.ignition.examples.python3.gateway;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.common.script.ScriptManager;
import com.inductiveautomation.ignition.common.script.hints.PropertiesFileDocProvider;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

/**
 * Gateway hook for Python 3 Integration module.
 * Manages the lifecycle of the Python process pool and registers scripting functions.
 */
public class GatewayHook extends AbstractGatewayModuleHook {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayHook.class);

    private GatewayContext gatewayContext;
    private Python3ProcessPool processPool;
    private PythonDistributionManager distributionManager;
    private Python3ScriptModule scriptModule;

    // Configuration
    private int poolSize = 3; // Default pool size
    private boolean autoDownload = true; // Auto-download Python by default

    @Override
    public void setup(GatewayContext context) {
        this.gatewayContext = context;
        LOGGER.info("Python 3 Integration module setup");

        // Load configuration
        loadConfiguration();

        // Initialize distribution manager
        distributionManager = new PythonDistributionManager(
                context.getSystemManager().getDataDir().toPath().resolve("python3-integration"),
                autoDownload
        );

        // TODO: Add test servlet registration once servlet API is figured out
        LOGGER.info("Test servlet not yet implemented - will test via Designer Script Console");
    }

    @Override
    public void startup(LicenseState licenseState) {
        LOGGER.info("Python 3 Integration module startup");

        try {
            // Get Python path (may download if needed)
            String pythonPath = distributionManager.getPythonPath();
            LOGGER.info("Using Python: {}", pythonPath);

            // Initialize process pool
            LOGGER.info("Initializing Python 3 process pool (size: {})", poolSize);
            processPool = new Python3ProcessPool(pythonPath, poolSize);

            LOGGER.info("Python 3 Integration module started successfully");
            LOGGER.info("Script module will now have access to initialized process pool");

        } catch (IOException e) {
            LOGGER.error("Failed to initialize Python 3 process pool", e);
            LOGGER.error("Options:");
            LOGGER.error("  1. Install Python 3.8+ on this server");
            LOGGER.error("  2. Enable auto-download: -Dignition.python3.autodownload=true");
            LOGGER.error("  3. Specify Python path: -Dignition.python3.path=/path/to/python3");
            // Don't throw - allow module to load but scripting functions will fail gracefully
        }
    }

    @Override
    public void shutdown() {
        LOGGER.info("Python 3 Integration module shutdown");

        // Shutdown process pool
        if (processPool != null) {
            try {
                processPool.shutdown();
            } catch (Exception e) {
                LOGGER.error("Error shutting down process pool", e);
            }
        }

        LOGGER.info("Python 3 Integration module shutdown complete");
    }

    @Override
    public void initializeScriptManager(ScriptManager manager) {
        super.initializeScriptManager(manager);

        LOGGER.info("Registering Python 3 scripting functions");

        // Create script module with lazy access to process pool
        // The module will become available once startup() initializes the pool
        scriptModule = new Python3ScriptModule(this);

        // Register under system.python3
        manager.addScriptModule(
                "system.python3",
                scriptModule,
                new PropertiesFileDocProvider()
        );

        // Initialize REST API endpoints with script module
        Python3RestEndpoints.initialize(scriptModule);
        LOGGER.info("REST API endpoints initialized");

        // TODO: Re-enable RPC registration once Designer scope is added
        // Register RPC implementation for Designer/Client access
        // try {
        //     gatewayContext.getRPCManager().registerHandler(
        //             Constants.MODULE_ID,
        //             Python3RpcFunctions.class,
        //             scriptModule
        //     );
        //     LOGGER.info("RPC handler registered for Designer/Client access");
        // } catch (Exception e) {
        //     LOGGER.error("Failed to register RPC handler", e);
        // }

        LOGGER.info("Python 3 scripting functions registered (pool will initialize during startup)");
    }

    @Override
    public void mountRouteHandlers(RouteGroup routes) {
        // Mount REST API endpoints at /data/python3integration/api/v1/* (Ignition 8.3 OpenAPI compliant)
        Python3RestEndpoints.mountRoutes(routes);
        LOGGER.info("Python3 REST API routes mounted at /data/python3integration/api/v1/");
    }

    @Override
    public Optional<String> getMountPathAlias() {
        // Use shorter alias for resources: /res/python3integration/ instead of full module ID
        return Optional.of("python3integration");
    }

    /**
     * Load configuration from system properties or environment variables
     */
    private void loadConfiguration() {
        // Load pool size configuration
        String configuredSize = System.getProperty("ignition.python3.poolsize");
        if (configuredSize != null) {
            try {
                poolSize = Integer.parseInt(configuredSize);
                LOGGER.info("Using configured pool size: {}", poolSize);
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid pool size configuration: {}, using default: {}", configuredSize, poolSize);
            }
        }

        // Load auto-download configuration
        String configuredAutoDownload = System.getProperty("ignition.python3.autodownload");
        if (configuredAutoDownload != null) {
            autoDownload = Boolean.parseBoolean(configuredAutoDownload);
            LOGGER.info("Auto-download: {}", autoDownload);
        }
    }

    /**
     * Get the current process pool (for testing/debugging)
     */
    public Python3ProcessPool getProcessPool() {
        return processPool;
    }

    /**
     * Get the distribution manager (for testing/debugging)
     */
    public PythonDistributionManager getDistributionManager() {
        return distributionManager;
    }

    /**
     * Check if Python 3 is available
     */
    public boolean isPython3Available() {
        return processPool != null && !processPool.isShutdown();
    }
}
