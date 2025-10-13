package com.inductiveautomation.ignition.examples.python3.gateway;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.common.script.ScriptManager;
import com.inductiveautomation.ignition.common.script.hints.PropertiesFileDocProvider;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Gateway hook for Python 3 Integration module.
 * Manages the lifecycle of the Python process pool and registers scripting functions.
 */
public class GatewayHook extends AbstractGatewayModuleHook {

    private static final Logger logger = LoggerFactory.getLogger(GatewayHook.class);

    private GatewayContext gatewayContext;
    private Python3ProcessPool processPool;
    private PythonDistributionManager distributionManager;

    // Configuration
    private int poolSize = 3; // Default pool size
    private boolean autoDownload = true; // Auto-download Python by default

    @Override
    public void setup(GatewayContext context) {
        this.gatewayContext = context;
        logger.info("Python 3 Integration module setup");

        // Load configuration
        loadConfiguration();

        // Initialize distribution manager
        distributionManager = new PythonDistributionManager(
                context.getSystemManager().getDataDir().toPath().resolve("python3-integration"),
                autoDownload
        );
    }

    @Override
    public void startup(LicenseState licenseState) {
        logger.info("Python 3 Integration module startup");

        try {
            // Get Python path (may download if needed)
            String pythonPath = distributionManager.getPythonPath();
            logger.info("Using Python: {}", pythonPath);

            // Initialize process pool
            logger.info("Initializing Python 3 process pool (size: {})", poolSize);
            processPool = new Python3ProcessPool(pythonPath, poolSize);

            logger.info("Python 3 Integration module started successfully");

        } catch (IOException e) {
            logger.error("Failed to initialize Python 3 process pool", e);
            logger.error("Options:");
            logger.error("  1. Install Python 3.8+ on this server");
            logger.error("  2. Enable auto-download: -Dignition.python3.autodownload=true");
            logger.error("  3. Specify Python path: -Dignition.python3.path=/path/to/python3");
            // Don't throw - allow module to load but scripting functions will fail gracefully
        }
    }

    @Override
    public void shutdown() {
        logger.info("Python 3 Integration module shutdown");

        // Shutdown process pool
        if (processPool != null) {
            try {
                processPool.shutdown();
            } catch (Exception e) {
                logger.error("Error shutting down process pool", e);
            }
        }

        logger.info("Python 3 Integration module shutdown complete");
    }

    @Override
    public void initializeScriptManager(ScriptManager manager) {
        super.initializeScriptManager(manager);

        logger.info("Registering Python 3 scripting functions");

        // Create script module
        Python3ScriptModule scriptModule;

        if (processPool != null) {
            scriptModule = new Python3ScriptModule(processPool, distributionManager);
        } else {
            // Create dummy module that will report Python 3 as unavailable
            logger.warn("Process pool not initialized, Python 3 functions will be unavailable");
            scriptModule = new Python3ScriptModule(null, distributionManager);
        }

        // Register under system.python3
        manager.addScriptModule(
                "system.python3",
                scriptModule,
                new PropertiesFileDocProvider()
        );

        logger.info("Python 3 scripting functions registered successfully");
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
                logger.info("Using configured pool size: {}", poolSize);
            } catch (NumberFormatException e) {
                logger.warn("Invalid pool size configuration: {}, using default: {}", configuredSize, poolSize);
            }
        }

        // Load auto-download configuration
        String configuredAutoDownload = System.getProperty("ignition.python3.autodownload");
        if (configuredAutoDownload != null) {
            autoDownload = Boolean.parseBoolean(configuredAutoDownload);
            logger.info("Auto-download: {}", autoDownload);
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
