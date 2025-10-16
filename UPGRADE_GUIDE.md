# Module Upgrade Guide

## Understanding Module Upgrades

The Python 3 Integration module follows Ignition's standard module upgrade mechanisms. However, certain types of changes require special upgrade procedures.

## When Seamless Upgrades Work

Upgrades work automatically for:
- Bug fixes (PATCH versions: 1.8.0 → 1.8.1)
- Gateway-only changes
- Minor enhancements to existing classes
- Configuration changes

## When Manual Upgrade Required

**Designer scope changes** may require manual upgrade when:
- New classes are added to designer.jar
- New UI components are introduced
- Significant Designer-side functionality changes

### Why This Happens

Ignition Designer caches JAR files for performance. When new classes are added to the Designer scope, the cached JARs may not include them until the Designer is restarted.

## Upgrade Procedures

### Standard Upgrade (PATCH versions)

For bug fixes and minor improvements:

1. Go to Gateway Config → System → Modules
2. Click "Install or Upgrade a Module"
3. Upload the new .modl file
4. Click "Install"
5. Gateway will restart automatically

**No Designer restart needed** for PATCH versions.

### Major Upgrade (MINOR/MAJOR versions)

For new features with Designer changes (like v1.7.x → v1.8.0):

**Method 1: Clean Install (Recommended)**
1. Uninstall the old module version
2. Wait for Gateway restart to complete
3. Install the new module version
4. Restart all Designer clients

**Method 2: Upgrade with Designer Restart**
1. Upload new .modl file and install
2. Close all Designer clients
3. Wait for Gateway restart
4. Reopen Designer clients

**Method 3: Force JAR Refresh (Advanced)**
1. Install new module version
2. Delete Designer JAR cache:
   - Windows: `%USERPROFILE%\.ignition\cache\`
   - Mac: `~/.ignition/cache/`
   - Linux: `~/.ignition/cache/`
3. Restart Designer

## Version History & Upgrade Paths

### v1.7.12 → v1.8.0
**Type**: MINOR (New Feature)
**Changes**: Added saved scripts functionality (new Designer classes)
**Upgrade**: Manual upgrade required (Method 1 or 2)
**Reason**: New classes added to designer.jar

### v1.7.11 → v1.7.12
**Type**: PATCH (Bug Fix)
**Changes**: Fixed stdout capture in Python bridge
**Upgrade**: Automatic
**Reason**: No Designer changes

### v1.7.0 → v1.7.11
**Type**: PATCH iterations
**Upgrade**: Automatic

## Module Configuration

The module is configured for seamless upgrades:

- **Static JAR Names**: ✓ (common.jar, gateway.jar, designer.jar)
- **Consistent Module ID**: ✓ (com.inductiveautomation.ignition.examples.python3)
- **SDK Version**: 0.4.1 (stable)
- **Free Module**: Yes (no license checks)

## Troubleshooting

### "Module does not contain a certificate"
**Solution**: Ensure Gateway is running Ignition 8.3.1-rc1 or later. Earlier versions had certificate validation issues with SDK 0.4.1.

### Designer Shows Old Version After Upgrade
**Solution**: Close and reopen Designer, or clear Designer cache.

### Saved Scripts Not Appearing
**Solution**: Ensure you're connected to the correct Gateway URL in the Designer IDE.

### Module Won't Install
**Solution**: Check Gateway logs at `<ignition>/logs/wrapper.log` for detailed error messages.

## Best Practices

1. **Test in Development First**: Always test upgrades in a dev environment
2. **Backup Scripts**: Export important saved scripts before upgrading
3. **Plan Downtime**: Major version upgrades may require coordinated Designer restarts
4. **Read Release Notes**: Check CHANGELOG.md for breaking changes

## Getting Help

If you encounter upgrade issues:

1. Check Gateway logs: `<ignition>/logs/wrapper.log`
2. Check Designer logs: Designer → Help → About → View Logs
3. Report issues with:
   - Current version
   - Target version
   - Error messages from logs
   - Upgrade method attempted
