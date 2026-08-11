#!/usr/bin/env bash
# Morpho build helper — sets env + builds. Usage: ./build.sh [debug|release|aab]
export JAVA_HOME="/usr/local/sdkman/candidates/java/17.0.13-tem"
export ANDROID_HOME="$HOME/android-sdk"

# free memory + kill stale daemons (avoids R8 OOM)
./gradlew --stop 2>/dev/null; pkill -9 -f java 2>/dev/null; sleep 4; sync

case "${1:-release}" in
  debug)   ./gradlew :app:assembleDebug --no-daemon -Dorg.gradle.daemon=false ;;
  release) ./gradlew :app:assembleRelease --no-daemon -Dorg.gradle.daemon=false ;;
  aab)     ./gradlew :app:bundleRelease --no-daemon -Dorg.gradle.daemon=false ;;
  *)       echo "Usage: ./build.sh [debug|release|aab]" ;;
esac
