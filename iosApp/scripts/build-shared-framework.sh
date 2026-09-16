#!/bin/sh
set -eu

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

find_java_home() {
  if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    echo "$JAVA_HOME"
    return 0
  fi

  for candidate in \
    "/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
    "/Applications/Android Studio Preview.app/Contents/jbr/Contents/Home" \
    "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" \
    "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" \
    "/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
  do
    if [ -x "$candidate/bin/java" ]; then
      echo "$candidate"
      return 0
    fi
  done

  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    if jh="$(/usr/libexec/java_home -v 17 2>/dev/null || /usr/libexec/java_home 2>/dev/null)"; then
      if [ -x "$jh/bin/java" ]; then
        echo "$jh"
        return 0
      fi
    fi
  fi

  return 1
}

JAVA_HOME="$(find_java_home)" || {
  echo "error: JAVA_HOME not found."
  echo "Install JDK 17+ with: brew install openjdk@17"
  echo "Or install Android Studio (bundled JBR)."
  exit 1
}
export JAVA_HOME
echo "Using JAVA_HOME=$JAVA_HOME"

if [ "${PLATFORM_NAME:-iphonesimulator}" = "iphonesimulator" ]; then
  GRADLE_TASK=":shared:linkDebugFrameworkIosSimulatorArm64"
  FRAMEWORK_OUT="shared/build/bin/iosSimulatorArm64/debugFramework/Shared.framework"
else
  GRADLE_TASK=":shared:linkDebugFrameworkIosArm64"
  FRAMEWORK_OUT="shared/build/bin/iosArm64/debugFramework/Shared.framework"
fi

chmod +x "$ROOT_DIR/gradlew"
"$ROOT_DIR/gradlew" "$GRADLE_TASK" --no-daemon

if [ ! -d "$FRAMEWORK_OUT" ]; then
  echo "error: Framework not found at $FRAMEWORK_OUT"
  exit 1
fi

echo "Built $FRAMEWORK_OUT"
