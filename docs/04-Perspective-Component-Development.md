# Ignition SDK - Perspective Component Development

## Overview

Perspective components are React-based web components that run in modern browsers. Unlike Vision (Java Swing), Perspective uses web technologies: React, TypeScript/JavaScript, HTML5, and CSS.

## Architecture

### Component Structure

A Perspective component module consists of:

1. **Gateway Scope**: Registers component with Perspective, handles metadata
2. **Designer Scope**: Design-time configuration, property editing
3. **Web Package**: React/TypeScript component code (runs in browser)

**No Client Scope**: Perspective uses web technologies, not Java client

### Technology Stack

- **React**: UI component library
- **TypeScript**: Type-safe JavaScript
- **Mobx**: State management (optional but common)
- **Webpack**: Module bundling
- **Node.js/npm**: Build tooling

## Project Structure

```
perspective-component/
├── build.gradle.kts
├── common/
│   └── src/main/java/
│       └── com/company/component/common/
│           └── ComponentMeta.java
├── gateway/
│   └── src/main/java/
│       └── com/company/component/gateway/
│           ├── GatewayHook.java
│           └── ComponentModelDelegate.java (optional)
├── designer/
│   └── src/main/java/
│       └── com/company/component/designer/
│           ├── DesignerHook.java
│           └── DesignDelegate.java (optional)
└── web/
    ├── package.json
    ├── webpack.config.js
    ├── tsconfig.json
    └── packages/
        ├── client/
        │   ├── typescript/
        │   │   ├── index.ts
        │   │   └── components/
        │   │       └── MyComponent.tsx
        │   └── package.json
        └── designer/
            ├── typescript/
            │   └── index.ts
            └── package.json
```

## Component Registration (Gateway)

### GatewayHook.java

```java
package com.company.component.gateway;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.perspective.gateway.api.PerspectiveContext;

public class GatewayHook extends AbstractGatewayModuleHook {
    
    private GatewayContext gatewayContext;
    private PerspectiveContext perspectiveContext;
    
    @Override
    public void setup(GatewayContext context) {
        this.gatewayContext = context;
    }
    
    @Override
    public void startup(LicenseState licenseState) {
        // Get Perspective context
        perspectiveContext = PerspectiveContext.get(gatewayContext);
        
        // Register component with Perspective
        perspectiveContext.getComponentRegistry()
            .registerComponent(ComponentMeta.DESCRIPTOR);
    }
    
    @Override
    public void shutdown() {
        if (perspectiveContext != null) {
            perspectiveContext.getComponentRegistry()
                .removeComponent(ComponentMeta.COMPONENT_ID);
        }
    }
}
```

### ComponentMeta.java (Common)

```java
package com.company.component.common;

import com.inductiveautomation.perspective.common.api.ComponentDescriptor;
import com.inductiveautomation.perspective.common.api.ComponentDescriptorImpl;

public class ComponentMeta {
    // Component ID - must match TypeScript
    public static final String COMPONENT_ID = "company.display.mycomponent";
    
    // Component descriptor
    public static final ComponentDescriptor DESCRIPTOR = ComponentDescriptorImpl.build()
        .withId(COMPONENT_ID)
        .withModuleId("com.company.perspective-component")
        .withSchema(ComponentMeta.class.getResource("/mycomponent.props.json"))
        .withPaletteCategory("Display")
        .withPaletteName("My Component")
        .withDefaultMetaName("myComponent")
        .withResources(ComponentMeta.class.getResource("/browser"))
        .build();
}
```

## Component Properties Schema

### mycomponent.props.json

Define component properties with JSON Schema:

```json
{
  "type": "object",
  "properties": {
    "text": {
      "type": "string",
      "default": "Hello World",
      "description": "The text to display"
    },
    "fontSize": {
      "type": "number",
      "default": 16,
      "minimum": 8,
      "maximum": 72,
      "description": "Font size in pixels"
    },
    "color": {
      "type": "string",
      "format": "color",
      "default": "#000000",
      "description": "Text color"
    },
    "enabled": {
      "type": "boolean",
      "default": true,
      "description": "Enable or disable component"
    },
    "data": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "name": { "type": "string" },
          "value": { "type": "number" }
        }
      },
      "default": []
    }
  }
}
```

## React Component (TypeScript)

### MyComponent.tsx

```typescript
import * as React from 'react';
import {
    Component,
    ComponentMeta,
    ComponentProps,
    PComponent,
    PropertyTree,
    SizeObject
} from '@inductiveautomation/perspective-client';

// Component ID - must match Java side
export const COMPONENT_TYPE = "company.display.mycomponent";

// Props interface matching JSON schema
export interface MyComponentProps {
    text: string;
    fontSize: number;
    color: string;
    enabled: boolean;
    data: Array<{name: string, value: number}>;
}

// Component class
export class MyComponent extends Component<ComponentProps<MyComponentProps>, any> {
    
    render() {
        // Extract props and emit function
        const { props, emit } = this.props;
        const { text, fontSize, color, enabled } = props;
        
        // CRITICAL: Root element must spread {...emit()}
        // This provides ref, events, style, className
        return (
            <div 
                {...emit()}
                style={{
                    fontSize: `${fontSize}px`,
                    color: color,
                    opacity: enabled ? 1 : 0.5,
                    padding: '10px'
                }}
            >
                {text}
            </div>
        );
    }
}

// Component metadata
export class MyComponentMeta implements ComponentMeta {
    
    getComponentType(): string {
        return COMPONENT_TYPE;
    }
    
    getViewComponent(): PComponent {
        return MyComponent;
    }
    
    getDefaultSize(): SizeObject {
        return {
            width: 200,
            height: 100
        };
    }
    
    // Map PropertyTree to component props
    getPropsReducer(tree: PropertyTree): MyComponentProps {
        return {
            text: tree.readString("text", "Hello World"),
            fontSize: tree.readNumber("fontSize", 16),
            color: tree.readString("color", "#000000"),
            enabled: tree.readBoolean("enabled", true),
            data: tree.readArray("data", [])
        };
    }
}
```

### index.ts (Client Registration)

```typescript
import { ComponentRegistry } from '@inductiveautomation/perspective-client';
import { MyComponent, MyComponentMeta, COMPONENT_TYPE } from './components/MyComponent';

// Register component when module loads
export function init() {
    ComponentRegistry.register(COMPONENT_TYPE, new MyComponentMeta());
}
```

## Advanced Features

### Using Component Events

```typescript
export class InteractiveComponent extends Component<ComponentProps<InteractiveProps>, any> {
    
    handleClick = () => {
        // Fire component event
        this.props.emit({
            type: 'dom',
            eventType: 'click'
        });
        
        // Or fire custom event
        this.props.componentEvents.fireComponentEvent('customEvent', {
            data: 'some value'
        });
    };
    
    render() {
        return (
            <button {...emit()} onClick={this.handleClick}>
                Click Me
            </button>
        );
    }
}
```

### State Management with Mobx

```typescript
import { observable, action, makeObservable } from 'mobx';

// Component store for complex state
class MyComponentStore {
    @observable
    counter: number = 0;
    
    constructor() {
        makeObservable(this);
    }
    
    @action
    increment() {
        this.counter++;
    }
}

export class StatefulComponent extends Component<ComponentProps<MyProps>, any> {
    private store: MyComponentStore;
    
    constructor(props: ComponentProps<MyProps>) {
        super(props);
        this.store = new MyComponentStore();
    }
    
    render() {
        return (
            <div {...this.props.emit()}>
                <p>Count: {this.store.counter}</p>
                <button onClick={() => this.store.increment()}>
                    Increment
                </button>
            </div>
        );
    }
}
```

### Gateway-Browser Communication

```typescript
// Component with message delegate
export class MessengerComponent extends Component<ComponentProps<MessengerProps>, any> {
    
    componentDidMount() {
        // Listen for messages from Gateway
        this.props.store.delegate.subscribe((message: any) => {
            console.log("Received from Gateway:", message);
            // Handle message
        });
    }
    
    sendToGateway() {
        // Send message to Gateway
        this.props.store.delegate.sendMessage({
            action: "getData",
            params: {}
        });
    }
    
    render() {
        return (
            <div {...this.props.emit()}>
                <button onClick={() => this.sendToGateway()}>
                    Send to Gateway
                </button>
            </div>
        );
    }
}
```

### Gateway Message Delegate

```java
public class ComponentModelDelegate extends AbstractComponentModelDelegate {
    
    @Override
    protected void onMessageReceived(JsonElement message) {
        // Handle message from browser
        JsonObject obj = message.getAsJsonObject();
        String action = obj.get("action").getAsString();
        
        // Process and send response
        JsonObject response = new JsonObject();
        response.addProperty("result", "success");
        sendMessage(response);
    }
}
```

## Designer Scope (Optional)

### Design Delegates

For design-time behavior:

```java
public class MyComponentDesignDelegate extends AbstractDesignDelegate {
    
    @Override
    public void setup(DesignContext context, 
                     ComponentDescriptor descriptor) {
        // Design-time setup
    }
    
    @Override
    public void onShutdown() {
        // Cleanup
    }
}
```

Register in DesignerHook:
```java
perspectiveContext.getDesignDelegateRegistry()
    .register(ComponentMeta.COMPONENT_ID, MyComponentDesignDelegate.class);
```

## Build Configuration

### package.json (Web)

```json
{
  "name": "@company/perspective-component",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "build": "lerna run build",
    "watch": "lerna run watch --parallel"
  },
  "workspaces": [
    "packages/*"
  ],
  "devDependencies": {
    "lerna": "^5.0.0",
    "webpack": "^5.0.0",
    "typescript": "^4.8.0"
  }
}
```

### package.json (Client Package)

```json
{
  "name": "@company/perspective-component-client",
  "version": "1.0.0",
  "main": "dist/index.js",
  "scripts": {
    "build": "webpack --mode production",
    "watch": "webpack --mode development --watch"
  },
  "dependencies": {
    "@inductiveautomation/perspective-client": "^2.0.0",
    "react": "^17.0.2",
    "mobx": "^6.0.0"
  },
  "devDependencies": {
    "@types/react": "^17.0.0",
    "typescript": "^4.8.0",
    "webpack": "^5.0.0",
    "webpack-cli": "^4.0.0",
    "ts-loader": "^9.0.0"
  }
}
```

### webpack.config.js

```javascript
const path = require('path');

module.exports = {
    entry: './typescript/index.ts',
    output: {
        path: path.resolve(__dirname, 'dist'),
        filename: 'index.js',
        library: {
            type: 'umd'
        }
    },
    module: {
        rules: [
            {
                test: /\.tsx?$/,
                use: 'ts-loader',
                exclude: /node_modules/
            }
        ]
    },
    resolve: {
        extensions: ['.tsx', '.ts', '.js']
    },
    externals: {
        'react': 'react',
        'mobx': 'mobx',
        '@inductiveautomation/perspective-client': '@inductiveautomation/perspective-client'
    }
};
```

### Gradle Integration

```kotlin
// Build web components before Java
tasks.named("processResources") {
    dependsOn("buildWeb")
}

tasks.register<Exec>("buildWeb") {
    workingDir("web")
    commandLine("npm", "run", "build")
}
```

## Component Features

### Quality Codes

Handle quality in component:

```typescript
render() {
    const { emit, store } = this.props;
    const quality = store.props.quality;
    
    if (quality && quality.isGood()) {
        // Render normally
        return <div {...emit()}>{this.props.props.value}</div>;
    } else {
        // Show quality indicator
        return (
            <div {...emit()} className="bad-quality">
                Bad Quality: {quality?.name}
            </div>
        );
    }
}
```

### Responsive Sizing

Components automatically respond to container size:

```typescript
render() {
    const { emit, width, height } = this.props;
    
    return (
        <div {...emit()} style={{
            width: '100%',
            height: '100%'
        }}>
            {/* Content scaled to width x height */}
        </div>
    );
}
```

### Styling

Use CSS-in-JS or CSS classes:

```typescript
render() {
    return (
        <div 
            {...this.props.emit({
                classes: ['my-component', 'custom-theme']
            })}
            style={{
                backgroundColor: '#f0f0f0',
                border: '1px solid #ccc'
            }}
        >
            Content
        </div>
    );
}
```

## Testing

### Unit Tests (Jest)

```typescript
import { render } from '@testing-library/react';
import { MyComponent } from './MyComponent';

describe('MyComponent', () => {
    it('renders text prop', () => {
        const props = {
            text: 'Test',
            fontSize: 16,
            color: '#000',
            enabled: true
        };
        
        const { getByText } = render(
            <MyComponent props={props} emit={() => ({})} />
        );
        
        expect(getByText('Test')).toBeInTheDocument();
    });
});
```

## Best Practices

### ✅ Do
- Always spread {...emit()} on root element
- Use TypeScript for type safety
- Keep component logic in React, business logic in Gateway
- Handle quality codes appropriately
- Test components in different containers
- Use Perspective's built-in styling system
- Optimize for mobile viewports
- Handle loading/error states

### ❌ Don't
- Put ref on root element (conflicts with emit)
- Use browser localStorage (not supported in artifacts)
- Make synchronous Gateway calls from component
- Ignore component lifecycle
- Hardcode dimensions
- Use deprecated React features
- Forget to handle undefined props

## Common Issues

**Component not appearing in palette**:
- Check component ID matches everywhere
- Verify registration in Gateway
- Check browser console for errors
- Verify webpack build succeeded

**Props not updating**:
- Check getPropsReducer implementation
- Verify JSON schema matches props interface
- Look for immutability issues

**Styling issues**:
- Ensure emit() is spread correctly
- Check CSS specificity
- Verify Tailwind classes are supported
- Test in different containers

**Performance problems**:
- Use React.memo for expensive renders
- Optimize Mobx observables
- Minimize Gateway communication
- Profile with React DevTools

## Resources

- **Perspective Client API**: Available in IDE autocomplete
- **Example Modules**: Check SDK examples repository
- **React Documentation**: https://react.dev/
- **TypeScript Handbook**: https://www.typescriptlang.org/docs/
- **Webpack Guide**: https://webpack.js.org/guides/
