# Ignition SDK - Vision Component Development

## Overview

Vision components are Java Swing components that run in Vision clients. They follow the JavaBeans specification and integrate with Ignition's Vision module for data binding, event handling, and more.

## Architecture

### Component Scopes

Vision components require:

1. **Client Scope**: Component runtime behavior (the actual Swing component)
2. **Designer Scope**: Design-time configuration (BeanInfo, property editors)
3. **Common Scope** (optional): Shared code between client and designer

**Gateway Scope**: Only if component needs server-side functionality (RPC)

### Technology Stack

- **Java Swing**: UI framework (JComponent, JPanel, etc.)
- **JavaBeans**: Component specification
- **BeanInfo**: Component metadata and property descriptors
- **Property Editors**: Custom UI for complex properties

## Project Structure

```
vision-component/
├── build.gradle.kts
├── common/ (optional)
│   └── src/main/java/
│       └── com/company/component/common/
│           └── Constants.java
├── client/
│   └── src/main/java/
│       └── com/company/component/client/
│           └── components/
│               └── HelloWorldComponent.java
└── designer/
    └── src/main/java/
        └── com/company/component/designer/
            ├── DesignerHook.java
            └── beaninfos/
                └── HelloWorldComponentBeanInfo.java
```

## Component Implementation (Client)

### Basic Component Class

```java
package com.company.component.client.components;

import com.inductiveautomation.vision.api.client.components.model.AbstractVisionComponent;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import javax.swing.*;

/**
 * A simple Vision component that displays text
 */
public class HelloWorldComponent extends AbstractVisionComponent {
    
    // Component properties
    private String text = "Hello World";
    private Color fillColor = Color.BLUE;
    private Color strokeColor = Color.BLACK;
    private float strokeWidth = 2.0f;
    
    // Constructor
    public HelloWorldComponent() {
        setPreferredSize(new Dimension(200, 100));
        setOpaque(false);
    }
    
    // Paint component
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                            RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Get component size
        int width = getWidth();
        int height = getHeight();
        
        // Draw text
        g2d.setColor(fillColor);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        int x = (width - textWidth) / 2;
        int y = (height + textHeight) / 2 - fm.getDescent();
        
        g2d.drawString(text, x, y);
        
        // Draw border
        if (strokeWidth > 0) {
            g2d.setColor(strokeColor);
            g2d.setStroke(new BasicStroke(strokeWidth));
            g2d.drawRect(0, 0, width - 1, height - 1);
        }
        
        g2d.dispose();
    }
    
    // Property: text (with bound property support)
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        String oldValue = this.text;
        this.text = text;
        firePropertyChange("text", oldValue, text);
        repaint();
    }
    
    // Property: fillColor
    public Color getFillColor() {
        return fillColor;
    }
    
    public void setFillColor(Color fillColor) {
        Color oldValue = this.fillColor;
        this.fillColor = fillColor;
        firePropertyChange("fillColor", oldValue, fillColor);
        repaint();
    }
    
    // Property: strokeColor
    public Color getStrokeColor() {
        return strokeColor;
    }
    
    public void setStrokeColor(Color strokeColor) {
        Color oldValue = this.strokeColor;
        this.strokeColor = strokeColor;
        firePropertyChange("strokeColor", oldValue, strokeColor);
        repaint();
    }
    
    // Property: strokeWidth
    public float getStrokeWidth() {
        return strokeWidth;
    }
    
    public void setStrokeWidth(float strokeWidth) {
        float oldValue = this.strokeWidth;
        this.strokeWidth = strokeWidth;
        firePropertyChange("strokeWidth", oldValue, strokeWidth);
        repaint();
    }
}
```

### Component with Data Binding

```java
public class DataBoundComponent extends AbstractVisionComponent {
    
    private int value = 0;
    
    public int getValue() {
        return value;
    }
    
    // Bound property - fires change events
    public void setValue(int value) {
        int oldValue = this.value;
        this.value = value;
        firePropertyChange("value", oldValue, value);
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawString("Value: " + value, 10, 20);
    }
}
```

### Component with Quality Monitoring

```java
import com.inductiveautomation.factorypmi.application.components.QualityMonitoring;
import com.inductiveautomation.vision.api.client.components.model.AbstractVisionComponent;

public class QualityAwareComponent extends AbstractVisionComponent 
        implements QualityMonitoring {
    
    private int qualityCode = 192; // GOOD quality
    
    @Override
    public int getQuality() {
        return qualityCode;
    }
    
    @Override
    public void setQuality(int quality) {
        int oldQuality = this.qualityCode;
        this.qualityCode = quality;
        firePropertyChange("quality", oldQuality, quality);
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Render differently based on quality
        if (qualityCode != 192) { // Not GOOD
            g.setColor(Color.RED);
            g.drawString("BAD QUALITY", 10, 20);
        } else {
            // Normal rendering
        }
    }
}
```

## BeanInfo (Designer)

### BeanInfo Class

```java
package com.company.component.designer.beaninfos;

import com.company.component.client.components.HelloWorldComponent;
import com.inductiveautomation.factorypmi.designer.property.customizers.DynamicPropertyProviderCustomizer;
import com.inductiveautomation.factorypmi.designer.property.customizers.StyleCustomizer;
import com.inductiveautomation.vision.api.designer.beans.CommonBeanInfo;
import com.inductiveautomation.vision.api.designer.beans.VisionBeanDescriptor;

import java.awt.Image;
import java.beans.IntrospectionException;

/**
 * BeanInfo for HelloWorldComponent
 * Describes component to the Vision module
 */
public class HelloWorldComponentBeanInfo extends CommonBeanInfo {
    
    public HelloWorldComponentBeanInfo() {
        // Call superclass with component class and customizers
        super(
            HelloWorldComponent.class,
            DynamicPropertyProviderCustomizer.VALUE_DESCRIPTOR,
            StyleCustomizer.VALUE_DESCRIPTOR
        );
    }
    
    @Override
    protected void initProperties() throws IntrospectionException {
        // Add common properties (name, position, size, etc.)
        super.initProperties();
        
        // Remove unused inherited properties
        removeProp("foreground");
        removeProp("background");
        removeProp("opaque");
        
        // Add component-specific properties
        
        // Text property (String, bindable, preferred)
        addProp(
            "text",                          // Property name
            "Text",                          // Display name
            "The text to display",           // Description
            CAT_DATA,                        // Category
            PREFERRED_MASK | BOUND_MASK      // Flags
        );
        
        // Fill color property
        addProp(
            "fillColor",
            "Fill Color",
            "The color to fill the text with",
            CAT_APPEARANCE,
            PREFERRED_MASK
        );
        
        // Stroke color property
        addProp(
            "strokeColor",
            "Stroke Color",
            "The border color",
            CAT_APPEARANCE
        );
        
        // Stroke width property
        addProp(
            "strokeWidth",
            "Stroke Width",
            "The border width in pixels",
            CAT_APPEARANCE
        );
    }
    
    @Override
    public Image getIcon(int kind) {
        // Provide component icon for palette
        // Return null to use default icon
        return null;
    }
    
    @Override
    protected void initDesc() {
        // Set component descriptor
        VisionBeanDescriptor desc = getBeanDescriptor();
        desc.setName("Hello World");
        desc.setDisplayName("Hello World");
        desc.setShortDescription("Displays custom text with styling");
    }
}
```

### Property Categories

```java
// Available categories:
CAT_DATA           // Data properties (values, datasets)
CAT_APPEARANCE     // Visual properties (colors, fonts)
CAT_BEHAVIOR       // Behavioral properties (enabled, visible)
CAT_LAYOUT         // Layout properties (position, size)
CAT_CUSTOMIZATION  // Advanced customization
```

### Property Flags

```java
// Available flags:
BOUND_MASK            // Property is bindable
PREFERRED_MASK        // Show in preferred properties
EXPERT_MASK           // Only in expert mode
HIDDEN_MASK           // Hide from property editor
NOT_TRANSLATABLE_MASK // Don't translate in localization
```

### Enum Properties

```java
// Add enum property
addEnumProp(
    "animationMode",
    "Animation Mode",
    "Controls animation behavior",
    CAT_BEHAVIOR,
    new int[]{0, 1, 2},                    // Enum values
    new String[]{"Off", "Slow", "Fast"}    // Display names
);
```

### Custom Property Editors

```java
import java.beans.PropertyEditorSupport;

public class CustomPropertyEditor extends PropertyEditorSupport {
    
    @Override
    public String getAsText() {
        return getValue().toString();
    }
    
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        // Parse text and set value
        setValue(parseValue(text));
    }
    
    @Override
    public Component getCustomEditor() {
        // Return custom UI component
        return new CustomEditorPanel(this);
    }
    
    @Override
    public boolean supportsCustomEditor() {
        return true;
    }
}

// Register in BeanInfo:
addProp("customProperty", "Custom Property", "Description", CAT_DATA);
prop("customProperty").setPropertyEditorClass(CustomPropertyEditor.class);
```

## Designer Hook

### Registering Components

```java
package com.company.component.designer;

import com.company.component.client.components.HelloWorldComponent;
import com.company.component.designer.beaninfos.HelloWorldComponentBeanInfo;
import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.designer.model.AbstractDesignerModuleHook;
import com.inductiveautomation.ignition.designer.model.DesignerContext;
import com.inductiveautomation.vision.api.designer.palette.ComponentPaletteCategory;
import com.inductiveautomation.vision.api.designer.palette.JavaBeanPaletteItem;
import com.inductiveautomation.vision.api.designer.palette.Palette;

public class DesignerHook extends AbstractDesignerModuleHook {
    
    private DesignerContext context;
    
    @Override
    public void setup(DesignerContext context) {
        this.context = context;
    }
    
    @Override
    public void startup(LicenseState licenseState) {
        // Get Vision module's component palette
        Palette palette = Palette.COMPONENT_PALETTE;
        
        // Get or create category
        ComponentPaletteCategory category = palette.getCategory("Custom");
        if (category == null) {
            category = new ComponentPaletteCategory("Custom", 100);
            palette.addCategory(category);
        }
        
        // Add component to palette
        category.addComponent(new JavaBeanPaletteItem(
            HelloWorldComponent.class,
            HelloWorldComponentBeanInfo.class,
            "Hello World",                    // Display name
            null,                             // Icon (null = default)
            "Custom hello world component"    // Description
        ));
    }
    
    @Override
    public void shutdown() {
        // Cleanup if needed
    }
}
```

## Advanced Features

### Event Handlers

```java
public class ButtonComponent extends AbstractVisionComponent {
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw button
    }
    
    @Override
    protected void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);
        
        if (e.getID() == MouseEvent.MOUSE_CLICKED) {
            // Fire action event
            fireActionEvent(new ActionEvent(
                this,
                ActionEvent.ACTION_PERFORMED,
                "clicked"
            ));
        }
    }
    
    // Convenience method to add action listeners
    public void addActionListener(ActionListener listener) {
        listenerList.add(ActionListener.class, listener);
    }
    
    private void fireActionEvent(ActionEvent event) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ActionListener.class) {
                ((ActionListener) listeners[i + 1]).actionPerformed(event);
            }
        }
    }
}
```

### Component Lifecycle

```java
import com.inductiveautomation.vision.api.client.components.model.AbstractVisionComponent;
import com.inductiveautomation.factorypmi.application.components.ComponentLifecycle;

public class LifecycleComponent extends AbstractVisionComponent 
        implements ComponentLifecycle {
    
    private Timer timer;
    
    @Override
    public void startupComponent(VisionClientContext context) {
        // Component is being added to runtime
        // Start background threads, timers, etc.
        timer = new Timer(1000, e -> {
            // Do something periodically
            repaint();
        });
        timer.start();
    }
    
    @Override
    public void shutdownComponent() {
        // Component is being removed
        // Stop threads, release resources
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }
}
```

### Custom Painting

```java
@Override
protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    
    Graphics2D g2d = (Graphics2D) g.create();
    
    // Enable antialiasing
    g2d.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON
    );
    
    // Set up transforms
    AffineTransform transform = g2d.getTransform();
    g2d.rotate(Math.toRadians(45), getWidth() / 2, getHeight() / 2);
    
    // Draw shapes
    g2d.setColor(Color.BLUE);
    g2d.fillRect(10, 10, 100, 50);
    
    // Restore transform
    g2d.setTransform(transform);
    
    // Always dispose of Graphics object
    g2d.dispose();
}
```

### Dynamic Property Provider

For components with dynamic properties:

```java
import com.inductiveautomation.factorypmi.application.components.DynamicPropertyProvider;

public class DynamicComponent extends AbstractVisionComponent 
        implements DynamicPropertyProvider {
    
    private Map<String, Object> dynamicProps = new HashMap<>();
    
    @Override
    public Object getDynamicProperty(String name) {
        return dynamicProps.get(name);
    }
    
    @Override
    public void setDynamicProperty(String name, Object value) {
        Object oldValue = dynamicProps.get(name);
        dynamicProps.put(name, value);
        firePropertyChange(name, oldValue, value);
    }
    
    @Override
    public Set<String> getDynamicPropertyNames() {
        return dynamicProps.keySet();
    }
}
```

## Gateway RPC (Optional)

If component needs Gateway communication:

```java
// Common interface
public interface ComponentRPC {
    String fetchData(String param);
}

// Gateway implementation
public class ComponentRPCImpl implements ComponentRPC {
    @Override
    public String fetchData(String param) {
        // Query database, etc.
        return "Result for: " + param;
    }
}

// Client usage
public class RpcComponent extends AbstractVisionComponent {
    
    private ComponentRPC rpc;
    
    @Override
    public void startupComponent(VisionClientContext context) {
        // Get RPC proxy
        rpc = ModuleRPCFactory.create(
            "com.company.module",
            ComponentRPC.class
        );
    }
    
    private void loadData() {
        String data = rpc.fetchData("param");
        // Use data
    }
}
```

## Best Practices

### ✅ Do
- Extend AbstractVisionComponent (or Panel/ScrollPane)
- Fire property change events for bound properties
- Use Graphics2D.create() and dispose()
- Implement ComponentLifecycle for resources
- Handle null/invalid property values gracefully
- Override getPreferredSize() for proper sizing
- Use SwingUtilities.invokeLater for thread safety
- Implement proper equals/hashCode if needed

### ❌ Don't
- Block in paint methods
- Perform heavy operations in getters
- Forget to repaint after property changes
- Hold references to contexts after shutdown
- Use System.out.println (use logger)
- Create threads without cleanup
- Modify component from non-EDT threads

## Common Patterns

### Thread-Safe Updates

```java
public void updateFromBackgroundThread(final int value) {
    SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
            setValue(value);
        }
    });
}
```

### Loading Indicator

```java
@Override
protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    
    if (isLoading) {
        g.setColor(Color.GRAY);
        g.drawString("Loading...", 10, 20);
    } else {
        // Normal rendering
    }
}
```

### Error Handling

```java
@Override
public void setValue(int value) {
    if (value < 0 || value > 100) {
        // Log error
        logger.warn("Invalid value: " + value);
        // Show indicator
        setBackground(Color.RED);
        return;
    }
    
    this.value = value;
    firePropertyChange("value", null, value);
    repaint();
}
```

## Testing

### Component Testing

```java
public class HelloWorldComponentTest {
    
    @Test
    public void testTextProperty() {
        HelloWorldComponent component = new HelloWorldComponent();
        component.setText("Test");
        assertEquals("Test", component.getText());
    }
    
    @Test
    public void testPropertyChangeEvent() {
        HelloWorldComponent component = new HelloWorldComponent();
        
        PropertyChangeListener listener = mock(PropertyChangeListener.class);
        component.addPropertyChangeListener(listener);
        
        component.setText("New Text");
        
        verify(listener).propertyChange(any(PropertyChangeEvent.class));
    }
}
```

## Troubleshooting

**Component not appearing in palette**:
- Check BeanInfo class name (must be ComponentName + BeanInfo)
- Verify DesignerHook registration
- Check for exceptions in Designer console

**Properties not editable**:
- Verify getter/setter methods exist
- Check BeanInfo property registration
- Ensure property isn't marked HIDDEN

**Component not rendering**:
- Check paintComponent implementation
- Verify repaint() is called after changes
- Check component size (getPreferredSize)

**Threading issues**:
- Always update UI from EDT
- Use SwingUtilities.invokeLater
- Implement proper lifecycle methods
