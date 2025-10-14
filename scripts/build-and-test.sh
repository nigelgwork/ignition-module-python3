#!/bin/bash
set -e

echo "=== Build and Test Workflow ==="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Navigate to project root
cd "$(dirname "$0")/.."

echo "Step 1: Cleanup Zone.Identifier files"
find . -name "*Zone.Identifier*" -type f -delete
echo -e "${GREEN}✓ Cleanup complete${NC}"

echo ""
echo "Step 2: Building module..."
cd python3-integration
./gradlew clean build --no-daemon
cd ..

if [ -f "python3-integration/build/Python3Integration-"*.modl ]; then
    MODULE_FILE=$(ls python3-integration/build/Python3Integration-*.modl | grep -v unsigned | head -1)
    echo -e "${GREEN}✓ Build successful: $(basename $MODULE_FILE)${NC}"
else
    echo -e "${RED}✗ Build failed - no module file found${NC}"
    exit 1
fi

echo ""
echo "Step 3: Starting test environment..."
docker-compose up -d

echo ""
echo "Step 4: Waiting for Gateway to be ready..."
until curl -s -o /dev/null -w "%{http_code}" "http://localhost:9088/StatusPing" | grep -q "200"; do
    echo "Waiting for Gateway..."
    sleep 5
done
echo -e "${GREEN}✓ Gateway is ready${NC}"

echo ""
echo "=== Ready for Testing ==="
echo ""
echo "Gateway URL: http://localhost:9088"
echo "Username: admin"
echo "Password: password"
echo ""
echo "Module file: $MODULE_FILE"
echo ""
echo "To install module, run: ./scripts/test-module.sh"
