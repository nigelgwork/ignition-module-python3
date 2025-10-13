# Ignition SDK - OPC-UA Device Driver Development

## Overview

OPC-UA device drivers extend Ignition's OPC-UA server to communicate with custom protocols and devices. The Device API allows you to:

- Connect to devices using custom protocols
- Expose device data through OPC-UA
- Integrate with Ignition's tag system
- Provide browsable node structure

## Architecture

### Components

1. **Device Type**: Defines the type of device (e.g., "ModbusTCP", "CustomProtocol")
2. **Device**: Instance of a device type with specific configuration
3. **Device Settings**: Persistent configuration (hostname, port, etc.)
4. **Device Driver**: Manages communication with physical device
5. **Node Structure**: OPC-UA nodes representing device data

### Scopes

OPC-UA drivers typically only need:
- **Gateway Scope**: All driver logic runs in Gateway

## Project Structure

```
opc-ua-device/
├── build.gradle.kts
├── common/ (optional)
│   └── src/main/java/
│       └── com/company/driver/common/
│           └── DriverConstants.java
└── gateway/
    └── src/main/java/
        └── com/company/driver/gateway/
            ├── GatewayHook.java
            ├── CustomDeviceType.java
            ├── CustomDevice.java
            └── CustomDeviceSettings.java
```

## Device Settings

### PersistentRecord Definition

```java
package com.company.driver.gateway;

import com.inductiveautomation.ignition.gateway.localdb.persistence.*;
import com.inductiveautomation.ignition.gateway.opcua.server.api.DeviceSettingsRecord;
import simpleorm.dataset.SFieldFlags;

/**
 * Device settings stored in internal database
 */
public class CustomDeviceSettings extends DeviceSettingsRecord {
    
    // Meta information
    public static final RecordMeta<CustomDeviceSettings> META = 
        new RecordMeta<>(
            CustomDeviceSettings.class,
            "CustomDeviceSettings"
        );
    
    // Device name field (inherited from DeviceSettingsRecord)
    
    // Custom fields
    public static final StringField Hostname = new StringField(
        META, "Hostname", SFieldFlags.SMANDATORY
    );
    
    public static final IntField Port = new IntField(
        META, "Port", SFieldFlags.SMANDATORY
    ).withDefault(502);
    
    public static final IntField Timeout = new IntField(
        META, "Timeout", SFieldFlags.SMANDATORY
    ).withDefault(5000);
    
    public static final BooleanField Enabled = new BooleanField(
        META, "Enabled"
    ).withDefault(true);
    
    @Override
    public RecordMeta<?> getMeta() {
        return META;
    }
    
    // Convenience getters
    public String getHostname() {
        return getString(Hostname);
    }
    
    public int getPort() {
        return getInt(Port);
    }
    
    public int getTimeout() {
        return getInt(Timeout);
    }
    
    public boolean isEnabled() {
        return getBoolean(Enabled);
    }
}
```

## Device Type

### DeviceType Implementation

```java
package com.company.driver.gateway;

import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.opcua.server.api.Device;
import com.inductiveautomation.ignition.gateway.opcua.server.api.DeviceContext;
import com.inductiveautomation.ignition.gateway.opcua.server.api.DeviceSettingsRecord;
import com.inductiveautomation.ignition.gateway.opcua.server.api.DeviceType;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;

/**
 * Defines the device type and creates device instances
 */
public class CustomDeviceType extends DeviceType {
    
    public static final String TYPE_ID = "CustomProtocol";
    
    private final GatewayContext gatewayContext;
    
    public CustomDeviceType(GatewayContext context) {
        this.gatewayContext = context;
    }
    
    @Override
    public String getTypeId() {
        return TYPE_ID;
    }
    
    @Override
    public LocalizedText getTypeName() {
        return LocalizedText.english("Custom Protocol Device");
    }
    
    @Override
    public LocalizedText getTypeDescription() {
        return LocalizedText.english(
            "Connects to devices using custom protocol"
        );
    }
    
    @Override
    public Class<? extends DeviceSettingsRecord> getSettingsRecordType() {
        return CustomDeviceSettings.class;
    }
    
    @Override
    public Device createDevice(
            DeviceContext deviceContext,
            DeviceSettingsRecord settings) {
        
        CustomDeviceSettings customSettings = (CustomDeviceSettings) settings;
        return new CustomDevice(
            gatewayContext,
            deviceContext,
            customSettings
        );
    }
}
```

## Device Implementation

### Device Class

```java
package com.company.driver.gateway;

import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.opcua.server.api.Device;
import com.inductiveautomation.ignition.gateway.opcua.server.api.DeviceContext;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * Device driver implementation
 */
public class CustomDevice implements Device {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final GatewayContext gatewayContext;
    private final DeviceContext deviceContext;
    private final CustomDeviceSettings settings;
    
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> pollingTask;
    
    // Device connection
    private volatile boolean connected = false;
    
    public CustomDevice(
            GatewayContext gatewayContext,
            DeviceContext deviceContext,
            CustomDeviceSettings settings) {
        
        this.gatewayContext = gatewayContext;
        this.deviceContext = deviceContext;
        this.settings = settings;
    }
    
    @Override
    public String getName() {
        return settings.getName();
    }
    
    @Override
    public String getStatus() {
        if (connected) {
            return "Connected";
        } else {
            return "Disconnected";
        }
    }
    
    @Override
    public void startup() {
        logger.info("Starting device: {}", getName());
        
        // Build node structure
        buildNodes();
        
        // Start communication
        connect();
        
        // Start polling
        startPolling();
    }
    
    @Override
    public void shutdown() {
        logger.info("Shutting down device: {}", getName());
        
        // Stop polling
        stopPolling();
        
        // Disconnect
        disconnect();
    }
    
    /**
     * Build OPC-UA node structure
     */
    private void buildNodes() {
        // Get device root node
        UaFolderNode deviceNode = deviceContext.getRootNode();
        
        // Create folder for device data
        UaFolderNode dataFolder = createFolder(
            deviceNode,
            "Data",
            "Device Data"
        );
        
        // Create variable nodes for data points
        createVariable(
            dataFolder,
            "Temperature",
            Identifiers.Double,
            0.0,
            true  // writable
        );
        
        createVariable(
            dataFolder,
            "Pressure",
            Identifiers.Double,
            0.0,
            false  // read-only
        );
        
        createVariable(
            dataFolder,
            "Status",
            Identifiers.String,
            "OK",
            false
        );
    }
    
    /**
     * Create folder node
     */
    private UaFolderNode createFolder(
            UaFolderNode parent,
            String name,
            String description) {
        
        NodeId nodeId = deviceContext.nodeId(name);
        
        UaFolderNode folder = new UaFolderNode(
            deviceContext.getNodeContext(),
            nodeId,
            deviceContext.qualifiedName(name),
            LocalizedText.english(description)
        );
        
        deviceContext.getNodeManager().addNode(folder);
        parent.addOrganizes(folder);
        
        return folder;
    }
    
    /**
     * Create variable node
     */
    private UaVariableNode createVariable(
            UaFolderNode parent,
            String name,
            NodeId dataType,
            Object initialValue,
            boolean writable) {
        
        NodeId nodeId = deviceContext.nodeId(name);
        
        UaVariableNode variable = new UaVariableNode.UaVariableNodeBuilder(
            deviceContext.getNodeContext())
            .setNodeId(nodeId)
            .setAccessLevel(writable ? 
                AccessLevel.READ_WRITE : AccessLevel.READ_ONLY)
            .setBrowseName(deviceContext.qualifiedName(name))
            .setDisplayName(LocalizedText.english(name))
            .setDataType(dataType)
            .setTypeDefinition(Identifiers.BaseDataVariableType)
            .build();
        
        // Set initial value
        variable.setValue(new DataValue(new Variant(initialValue)));
        
        // Add write handler if writable
        if (writable) {
            variable.getFilterChain().addLast(
                new AttributeLoggingFilter(),
                AttributeFilters.getValue(
                    ctx -> writeToDevice(name, ctx.getValue().getValue())
                )
            );
        }
        
        deviceContext.getNodeManager().addNode(variable);
        parent.addOrganizes(variable);
        
        return variable;
    }
    
    /**
     * Connect to device
     */
    private void connect() {
        try {
            String hostname = settings.getHostname();
            int port = settings.getPort();
            
            logger.info("Connecting to {}:{}", hostname, port);
            
            // Implement your protocol connection here
            // For example: TCP socket, serial port, etc.
            
            connected = true;
            logger.info("Connected to device");
            
        } catch (Exception e) {
            logger.error("Failed to connect to device", e);
            connected = false;
        }
    }
    
    /**
     * Disconnect from device
     */
    private void disconnect() {
        connected = false;
        // Close connections, release resources
    }
    
    /**
     * Start polling device data
     */
    private void startPolling() {
        executor = Executors.newSingleThreadScheduledExecutor();
        
        pollingTask = executor.scheduleAtFixedRate(
            this::pollDevice,
            0,              // Initial delay
            1000,           // Period (1 second)
            TimeUnit.MILLISECONDS
        );
    }
    
    /**
     * Stop polling
     */
    private void stopPolling() {
        if (pollingTask != null) {
            pollingTask.cancel(false);
        }
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.warn("Interrupted waiting for executor shutdown");
            }
        }
    }
    
    /**
     * Poll device for data
     */
    private void pollDevice() {
        if (!connected) {
            return;
        }
        
        try {
            // Read data from device
            double temperature = readTemperature();
            double pressure = readPressure();
            String status = readStatus();
            
            // Update OPC-UA nodes
            updateNode("Data/Temperature", temperature);
            updateNode("Data/Pressure", pressure);
            updateNode("Data/Status", status);
            
        } catch (Exception e) {
            logger.error("Error polling device", e);
        }
    }
    
    /**
     * Update node value
     */
    private void updateNode(String browsePath, Object value) {
        UaVariableNode node = deviceContext.getNode(
            deviceContext.nodeId(browsePath)
        );
        
        if (node != null) {
            node.setValue(new DataValue(new Variant(value)));
        }
    }
    
    /**
     * Write value to device
     */
    private DataValue writeToDevice(String nodeName, Object value) {
        try {
            logger.info("Writing {} to device: {}", value, nodeName);
            
            // Implement device write here
            
            return new DataValue(new Variant(value));
            
        } catch (Exception e) {
            logger.error("Error writing to device", e);
            // Return bad status
            return new DataValue(StatusCodes.Bad_UnexpectedError);
        }
    }
    
    // Implement protocol-specific methods
    private double readTemperature() {
        // Read from device
        return 25.0;
    }
    
    private double readPressure() {
        // Read from device
        return 101.3;
    }
    
    private String readStatus() {
        return connected ? "OK" : "Disconnected";
    }
}
```

## Gateway Hook

### Registering Device Type

```java
package com.company.driver.gateway;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.opcua.server.api.AbstractDeviceModuleHook;
import com.inductiveautomation.ignition.gateway.opcua.server.api.DeviceType;

public class GatewayHook extends AbstractDeviceModuleHook {
    
    private GatewayContext gatewayContext;
    private CustomDeviceType deviceType;
    
    @Override
    public void setup(GatewayContext context) {
        this.gatewayContext = context;
        
        // Initialize device type
        deviceType = new CustomDeviceType(context);
        
        // Register persistent record schema
        context.getSchemaUpdater()
            .updatePersistentRecords(CustomDeviceSettings.META);
    }
    
    @Override
    public void startup(LicenseState licenseState) {
        // Module startup
    }
    
    @Override
    public void shutdown() {
        // Cleanup
    }
    
    @Override
    protected DeviceType getDeviceType() {
        return deviceType;
    }
}
```

## Advanced Features

### Subscription Management

For efficient data delivery based on client subscriptions:

```java
public class CustomDevice implements Device {
    
    private final SubscriptionModel subscriptionModel;
    
    public CustomDevice(...) {
        // Initialize subscription model
        subscriptionModel = new SubscriptionModel() {
            @Override
            public void onDataItemsCreated(List<DataItem> items) {
                // Items added to subscription
                for (DataItem item : items) {
                    logger.info("Subscribed to: {}", item.getReadValueId());
                }
            }
            
            @Override
            public void onDataItemsModified(List<DataItem> items) {
                // Subscription parameters changed
            }
            
            @Override
            public void onDataItemsDeleted(List<DataItem> items) {
                // Items removed from subscription
            }
        };
    }
    
    @Override
    public SubscriptionModel getSubscriptionModel() {
        return subscriptionModel;
    }
}
```

### Browse Path Support

Enable browsing by path:

```java
@Override
public void browse(
        BrowseContext context,
        ViewDescription view,
        NodeId nodeId) {
    
    // Find node by browse path
    UaNode node = deviceContext.getNodeManager().get(nodeId);
    
    if (node != null) {
        // Add references to browse result
        node.getReferences().stream()
            .filter(ref -> context.getRequest().getIncludeSubtypes() ||
                          ref.isForward())
            .forEach(ref -> {
                context.success(new ReferenceDescription(
                    ref.getReferenceTypeId(),
                    ref.isForward(),
                    ref.getTargetNodeId().expanded(),
                    // ... other fields
                ));
            });
    }
}
```

### Historical Data

Implement historical data access:

```java
@Override
public void historyRead(
        HistoryReadContext context,
        HistoryReadDetails details,
        TimestampsToReturn timestamps,
        List<HistoryReadValueId> readValueIds) {
    
    // Read historical data
    for (HistoryReadValueId valueId : readValueIds) {
        NodeId nodeId = valueId.getNodeId();
        
        // Query historical data
        List<DataValue> history = queryHistory(nodeId, details);
        
        // Return results
        context.success(nodeId, new HistoryData(
            new DataValue[history.size()]
        ));
    }
}
```

## Best Practices

### ✅ Do
- Use AbstractDeviceModuleHook for Gateway hook
- Implement proper connection management
- Handle disconnections and reconnections
- Use scheduled executors for polling
- Implement proper error handling
- Log connection events
- Update node status appropriately
- Clean up resources in shutdown()

### ❌ Don't
- Block in startup() method
- Poll faster than device can respond
- Create nodes dynamically during operation
- Hold locks during I/O operations
- Ignore connection errors
- Forget to stop threads
- Use System.out.println (use logger)

## Testing

### Device Testing

```java
public class CustomDeviceTest {
    
    @Test
    public void testConnection() throws Exception {
        CustomDevice device = new CustomDevice(...);
        device.startup();
        
        // Wait for connection
        Thread.sleep(1000);
        
        assertEquals("Connected", device.getStatus());
        
        device.shutdown();
    }
    
    @Test
    public void testNodeCreation() throws Exception {
        CustomDevice device = new CustomDevice(...);
        device.startup();
        
        // Verify nodes exist
        UaVariableNode node = deviceContext.getNode(
            deviceContext.nodeId("Data/Temperature")
        );
        
        assertNotNull(node);
        
        device.shutdown();
    }
}
```

## Troubleshooting

**Device not appearing**:
- Check DeviceType registration
- Verify persistent record schema
- Check Gateway logs

**Nodes not browsable**:
- Verify node structure creation
- Check node references
- Ensure nodes added to NodeManager

**Connection issues**:
- Check network connectivity
- Verify device settings
- Review connection logs
- Test protocol independently

**Performance problems**:
- Reduce polling frequency
- Optimize node updates
- Use subscription model
- Profile with JVM tools
