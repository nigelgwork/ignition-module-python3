package com.inductiveautomation.ignition.examples.python3.designer;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.common.script.ScriptManager;
import com.inductiveautomation.ignition.common.script.hints.PropertiesFileDocProvider;
import com.inductiveautomation.ignition.designer.model.AbstractDesignerModuleHook;
import com.inductiveautomation.ignition.designer.model.DesignerContext;
import com.inductiveautomation.ignition.examples.python3.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Designer hook for Python 3 Integration module.
 *
 * This hook registers the scripting functions in the Designer scope.
 * RPC initialization is done lazily to avoid blocking Designer startup.
 */
public class DesignerHook extends AbstractDesignerModuleHook {
    private static final Logger LOGGER = LoggerFactory.getLogger(DesignerHook.class);

    private DesignerContext designerContext;
    private Python3DesignerScriptModule scriptModule;

    public void setup(DesignerContext context, LicenseState activationState) {
        this.designerContext = context;
        LOGGER.info("Python 3 Integration Designer module setup");
    }

    public void startup(LicenseState activationState) {
        LOGGER.info("Python 3 Integration Designer module startup");

        try {
            // Create script module with lazy RPC initialization
            scriptModule = new Python3DesignerScriptModule(designerContext);

            // Register script module
            ScriptManager scriptManager = designerContext.getScriptManager();
            scriptManager.addScriptModule(
                    Constants.SCRIPT_NAMESPACE,
                    scriptModule,
                    new PropertiesFileDocProvider()
            );

            LOGGER.info("Python 3 scripting functions registered in Designer scope");
            LOGGER.info("Functions will connect to Gateway via RPC on first use");

        } catch (Exception e) {
            LOGGER.error("Failed to register Python 3 script module in Designer", e);
        }
    }

    @Override
    public void shutdown() {
        LOGGER.info("Python 3 Integration Designer module shutdown");

        if (scriptModule != null) {
            scriptModule.shutdown();
        }

        LOGGER.info("Python 3 Integration Designer module shut down successfully");
    }
}
