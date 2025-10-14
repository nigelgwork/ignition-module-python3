# Module Signing for Gaskony Python 3 Integration

This document explains how to sign the Ignition module with Gaskony's self-signed certificates.

## Overview

The module requires signing to be installed in Ignition Gateway. We use self-signed certificates for development and testing.

## Quick Start

Run this script to generate all required signing files:

```bash
cd python3-integration

# Generate keystore and certificates
./scripts/generate-signing-certs.sh
```

This will create:
- `keystore.jks` - Keystore with private key
- `certificate.der` - Public certificate
- `gradle.properties` - Build configuration with signing settings

## Manual Setup

If you prefer to generate the certificates manually:

### Step 1: Generate Keystore

```bash
keytool -genkeypair \
  -alias gaskony \
  -keyalg RSA \
  -keysize 2048 \
  -validity 3650 \
  -keystore keystore.jks \
  -storepass gaskony2024 \
  -keypass gaskony2024 \
  -dname "CN=Gaskony, OU=Development, O=Gaskony, L=City, ST=State, C=US"
```

### Step 2: Export Certificate

```bash
keytool -exportcert \
  -alias gaskony \
  -keystore keystore.jks \
  -storepass gaskony2024 \
  -file certificate.der
```

### Step 3: Create gradle.properties

Create `python3-integration/gradle.properties`:

```properties
# Ignition Module Signing Configuration
ignition.signing.keystoreFile=keystore.jks
ignition.signing.keystorePassword=gaskony2024
ignition.signing.certFile=certificate.der
ignition.signing.certAlias=gaskony
ignition.signing.certPassword=gaskony2024
```

## Build Signed Module

```bash
cd python3-integration
./gradlew clean build
```

The build will produce:
- `build/Python3Integration-1.0.0.modl` - **Signed module** (ready for installation)
- `build/Python3Integration-1.0.0.unsigned.modl` - Unsigned backup

## Verify Signing

Check that the module is signed:

```bash
unzip -l build/Python3Integration-1.0.0.modl | grep -E "certificates|signatures"
```

Should show:
```
certificates.p7b
signatures.properties
```

## Security Notes

### Development Certificates

These are **self-signed development certificates** with publicly known passwords:
- **DO NOT** use in production
- **DO NOT** distribute signed modules to customers with these certificates
- For production, obtain proper code signing certificates from a trusted Certificate Authority

### File Security

The following files contain sensitive information and are excluded from git:
- `keystore.jks` - Private key
- `certificate.der` - Certificate
- `gradle.properties` - Passwords
- `sign.props` - Alternative signing configuration

## CI/CD Integration

For GitHub Actions to build signed modules, you need to:

### Option 1: Generate Certificates in CI

Add this step to `.github/workflows/ci.yml`:

```yaml
- name: Generate signing certificates
  working-directory: python3-integration
  run: |
    keytool -genkeypair -alias gaskony -keyalg RSA -keysize 2048 \
      -validity 3650 -keystore keystore.jks \
      -storepass gaskony2024 -keypass gaskony2024 \
      -dname "CN=Gaskony, OU=Development, O=Gaskony, L=City, ST=State, C=US"

    keytool -exportcert -alias gaskony -keystore keystore.jks \
      -storepass gaskony2024 -file certificate.der

    cat > gradle.properties <<EOF
    ignition.signing.keystoreFile=keystore.jks
    ignition.signing.keystorePassword=gaskony2024
    ignition.signing.certFile=certificate.der
    ignition.signing.certAlias=gaskony
    ignition.signing.certPassword=gaskony2024
    EOF
```

### Option 2: Use GitHub Secrets (Production)

For production certificates:

1. Store certificates securely in GitHub Secrets
2. Base64 encode the keystore: `base64 keystore.jks > keystore.b64`
3. Add to GitHub Secrets:
   - `SIGNING_KEYSTORE_B64` - Base64 encoded keystore
   - `SIGNING_CERT_B64` - Base64 encoded certificate
   - `SIGNING_PASSWORD` - Certificate password

4. Update CI workflow:

```yaml
- name: Setup signing certificates
  working-directory: python3-integration
  env:
    KEYSTORE_B64: ${{ secrets.SIGNING_KEYSTORE_B64 }}
    CERT_B64: ${{ secrets.SIGNING_CERT_B64 }}
    PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
  run: |
    echo "$KEYSTORE_B64" | base64 -d > keystore.jks
    echo "$CERT_B64" | base64 -d > certificate.der

    cat > gradle.properties <<EOF
    ignition.signing.keystoreFile=keystore.jks
    ignition.signing.keystorePassword=$PASSWORD
    ignition.signing.certFile=certificate.der
    ignition.signing.certAlias=gaskony
    ignition.signing.certPassword=$PASSWORD
    EOF
```

## Production Certificates

For production deployment, obtain certificates from a Certificate Authority:

1. Generate Certificate Signing Request (CSR):
```bash
keytool -certreq -alias gaskony -keystore keystore.jks \
  -file gaskony.csr -storepass YOUR_SECURE_PASSWORD
```

2. Submit CSR to Certificate Authority
3. Receive signed certificate
4. Import into keystore:
```bash
keytool -importcert -alias gaskony -keystore keystore.jks \
  -file signed-cert.cer -storepass YOUR_SECURE_PASSWORD
```

5. Export for Ignition SDK:
```bash
keytool -exportcert -alias gaskony -keystore keystore.jks \
  -file certificate.der -storepass YOUR_SECURE_PASSWORD
```

## Troubleshooting

### Build fails with "keystore not found"

Ensure `gradle.properties` exists in `python3-integration/` directory with correct paths.

### Module won't install: "Invalid signature"

1. Verify certificate files exist
2. Check passwords match in gradle.properties
3. Rebuild: `./gradlew clean build`

### CI build fails on signing

1. Verify signing certificates are generated before build step
2. Check gradle.properties is created correctly
3. Ensure file paths are relative to python3-integration/ directory

## Questions?

For issues with module signing, check:
- [Ignition SDK Documentation](https://www.sdk-docs.inductiveautomation.com/)
- [Module Build Guide](IGNITION_MODULE_BUILD_GUIDE.md)
- [Troubleshooting Guide](MODULE_BUILD_TROUBLESHOOTING.md)
