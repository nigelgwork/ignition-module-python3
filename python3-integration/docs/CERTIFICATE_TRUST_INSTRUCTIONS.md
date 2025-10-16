# Certificate Trust Instructions for Python 3 Integration Module

## Problem
Ignition Gateway reports "module does not contain a certificate" even though the module is properly signed with a self-signed certificate.

## Certificate Information
- **Issuer:** Gaskony Development
- **Valid:** Oct 14, 2025 - Oct 12, 2035
- **Algorithm:** SHA256withRSA (2048-bit)
- **Certificate File:** `/tmp/gaskony-cert.pem`

## Solution Options

### Option 1: Enable "Allow Unsigned Modules" (Temporary Testing)

1. Open Gateway webpage: `http://localhost:9088`
2. Navigate to: **Config → Security → Modules**
3. Enable: **"Allow Unsigned Modules"**
4. Save changes
5. Try installing the module again

**Note:** This is for testing only. Not recommended for production.

### Option 2: Import Certificate into Ignition Trust Store

1. Export the certificate (already done):
   ```bash
   # Certificate available at: /tmp/gaskony-cert.pem
   ```

2. Import into Ignition's JVM trust store:
   ```bash
   keytool -import \
     -alias gaskony-module-signer \
     -file /tmp/gaskony-cert.pem \
     -keystore $IGNITION_HOME/lib/runtime/jre/lib/security/cacerts \
     -storepass changeit
   ```

3. Restart Ignition Gateway

### Option 3: Use Inductive Automation's Module Signing Service

For production use, consider using IA's official signing service:
- Contact: Inductive Automation support
- Service: Module signing with trusted CA certificate
- Cost: May be free for community modules

### Option 4: Check Gateway Configuration

Check if Gateway has module signature validation disabled:

1. Open: `$IGNITION_HOME/data/ignition.conf`
2. Look for: `-Dignition.allowunsignedmodules=true`
3. If not present, add it to Java additional options
4. Restart Gateway

## Verification

After applying a solution, verify the module installs:

```bash
# Check module loaded
curl http://localhost:9088/StatusPing

# Check Gateway logs
tail -f $IGNITION_HOME/logs/wrapper.log | grep -i "python3\|certificate"
```

## Module Files

- **Signed Module:** `/modules/ignition-module-python3/python3-integration/build/Python3Integration-1.7.7.modl`
- **Certificate:** `/tmp/gaskony-cert.pem`
- **Keystore:** `/modules/ignition-module-python3/python3-integration/keystore.jks`

## Next Steps

1. Try Option 1 first (easiest for testing)
2. If that works, implement Option 2 or 3 for production
3. Report back which option worked for your Gateway configuration
