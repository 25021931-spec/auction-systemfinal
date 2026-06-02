#!/bin/bash
# ============================================================
#  Auction System - Start Client
#  Usage: ./run-client.sh
# ============================================================

echo "======================================"
echo "  🔨 AUCTION SYSTEM - CLIENT"
echo "======================================"

# Option 1: Run via Maven JavaFX plugin (recommended for dev)
if command -v mvn &> /dev/null; then
  echo "🚀 Launching client via Maven..."
  mvn -pl client javafx:run
else
  # Option 2: Run JAR (needs JavaFX on PATH)
  JAR="client/target/auction-client-jar-with-dependencies.jar"
  if [ ! -f "$JAR" ]; then
    echo "❌ JAR not found. Please run: mvn package"
    exit 1
  fi
  echo "🚀 Launching client via JAR..."
  java -jar "$JAR"
fi
