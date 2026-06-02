#!/bin/bash
# ============================================================
#  Auction System - Run Tests
# ============================================================
echo "======================================"
echo "  🧪 RUNNING UNIT TESTS"
echo "======================================"

mvn -pl shared install -q && mvn -pl server test

echo ""
echo "Test reports: server/target/surefire-reports/"
