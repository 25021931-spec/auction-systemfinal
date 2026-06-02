#!/bin/bash
# ============================================================
#  Auction System - Start Server
#  Usage: ./run-server.sh [port]
#  Default port: 9090
# ============================================================

PORT=${1:-9090}
JAR="server/target/auction-server-jar-with-dependencies.jar"

echo "======================================"
echo "  🔨 AUCTION SYSTEM - SERVER"
echo "======================================"

# Check if JAR exists, build if not
if [ ! -f "$JAR" ]; then
  echo "📦 JAR not found. Building..."
  mvn -pl shared install -q && mvn -pl server package -DskipTests -q
  if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
  fi
fi

echo "🚀 Starting server on port $PORT..."
echo "   Admin login: admin / admin123"
echo "   Press Ctrl+C to stop"
echo "======================================"

java -jar "$JAR" "$PORT"
