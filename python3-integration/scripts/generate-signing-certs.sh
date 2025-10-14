#!/bin/bash
# Generate self-signed certificates for Gaskony Ignition module

set -e

echo "========================================="
echo "Gaskony Module Signing Certificate Setup"
echo "========================================="
echo ""

# Configuration
ALIAS="gaskony"
PASSWORD="gaskony2024"
VALIDITY_DAYS=3650
DNAME="CN=Gaskony, OU=Development, O=Gaskony, L=City, ST=State, C=US"

# File names
KEYSTORE="keystore.jks"
CERTIFICATE="certificate.der"
GRADLE_PROPS="gradle.properties"

# Check if files already exist
if [ -f "$KEYSTORE" ] || [ -f "$CERTIFICATE" ] || [ -f "$GRADLE_PROPS" ]; then
    echo "⚠️  Signing files already exist:"
    [ -f "$KEYSTORE" ] && echo "  - $KEYSTORE"
    [ -f "$CERTIFICATE" ] && echo "  - $CERTIFICATE"
    [ -f "$GRADLE_PROPS" ] && echo "  - $GRADLE_PROPS"
    echo ""
    read -p "Overwrite existing files? (y/N): " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Aborted."
        exit 1
    fi
    echo ""
fi

# Step 1: Generate keystore
echo "📝 Step 1: Generating keystore..."
keytool -genkeypair \
    -alias "$ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity "$VALIDITY_DAYS" \
    -keystore "$KEYSTORE" \
    -storepass "$PASSWORD" \
    -keypass "$PASSWORD" \
    -dname "$DNAME"

echo "✅ Keystore generated: $KEYSTORE"
echo ""

# Step 2: Export certificate
echo "📝 Step 2: Exporting certificate..."
keytool -exportcert \
    -alias "$ALIAS" \
    -keystore "$KEYSTORE" \
    -storepass "$PASSWORD" \
    -file "$CERTIFICATE"

echo "✅ Certificate exported: $CERTIFICATE"
echo ""

# Step 3: Create gradle.properties
echo "📝 Step 3: Creating gradle.properties..."
cat > "$GRADLE_PROPS" <<EOF
# Ignition Module Signing Configuration
# Self-signed certificates for Gaskony

ignition.signing.keystoreFile=$KEYSTORE
ignition.signing.keystorePassword=$PASSWORD
ignition.signing.certFile=$CERTIFICATE
ignition.signing.certAlias=$ALIAS
ignition.signing.certPassword=$PASSWORD
EOF

echo "✅ Configuration created: $GRADLE_PROPS"
echo ""

# Summary
echo "========================================="
echo "✅ Certificate generation complete!"
echo "========================================="
echo ""
echo "Created files:"
echo "  - $KEYSTORE (private key)"
echo "  - $CERTIFICATE (public certificate)"
echo "  - $GRADLE_PROPS (build configuration)"
echo ""
echo "Next steps:"
echo "  1. Build module: ./gradlew clean build"
echo "  2. Check output: ls -lh build/*.modl"
echo ""
echo "⚠️  Security Notice:"
echo "  These are DEVELOPMENT certificates only!"
echo "  Do NOT use for production distribution."
echo ""
