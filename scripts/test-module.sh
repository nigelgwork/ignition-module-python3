#!/bin/bash
set -e

echo "=== Ignition Module Testing Script ==="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
GATEWAY_URL="http://localhost:9088"
USERNAME="admin"
PASSWORD="password"
MODULE_PATH="../python3-integration/build"

echo "Step 1: Check if Ignition Gateway is running..."
if curl -s -o /dev/null -w "%{http_code}" "${GATEWAY_URL}/StatusPing" | grep -q "200"; then
    echo -e "${GREEN}✓ Gateway is running${NC}"
else
    echo -e "${RED}✗ Gateway is not running${NC}"
    echo "Starting Gateway with docker-compose..."
    cd ..
    docker-compose up -d
    echo "Waiting for Gateway to start (this may take a minute)..."
    sleep 30
    cd scripts
fi

echo ""
echo "Step 2: Finding latest module file..."
LATEST_MODULE=$(ls -t "${MODULE_PATH}"/*.modl | grep -v unsigned | head -1)
if [ -z "$LATEST_MODULE" ]; then
    echo -e "${RED}✗ No module file found in ${MODULE_PATH}${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Found module: $(basename $LATEST_MODULE)${NC}"

echo ""
echo "Step 3: Checking current installed modules..."
# This would use the Ignition API to list modules
# For now, just inform the user
echo -e "${YELLOW}! Manual step: Check Config → System → Modules for existing version${NC}"
echo -e "${YELLOW}! If Python 3 Integration is installed, uninstall it first${NC}"

echo ""
echo "Step 4: Module ready for installation"
echo -e "${GREEN}✓ Module location: $LATEST_MODULE${NC}"
echo ""
echo "Next steps:"
echo "1. Open browser: ${GATEWAY_URL}"
echo "2. Login with: ${USERNAME} / ${PASSWORD}"
echo "3. Go to: Config → System → Modules"
echo "4. Click: Install or Upgrade a Module"
echo "5. Upload: $LATEST_MODULE"
echo "6. Restart Gateway when prompted"
echo ""
echo "After installation:"
echo "7. Open Designer"
echo "8. Create/Open Gateway Scripting Project"
echo "9. Tools → Script Console"
echo "10. Test: system.python3.isAvailable()"
