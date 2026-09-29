#!/bin/sh
# Gradle wrapper launcher.
# 1) If gradle/wrapper/gradle-wrapper.jar exists, behave like the standard wrapper.
# 2) Otherwise bootstrap the exact Gradle version from gradle-wrapper.properties
#    (downloaded once into ~/.gradle/bootstrap) so no manual Gradle install is needed.
set -e

APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
PROPS="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD=java
fi

if [ -f "$JAR" ]; then
  exec "$JAVACMD" -Xmx64m -Xms64m -Dorg.gradle.appname=gradlew \
    -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
fi

URL=$(sed -n 's/^distributionUrl=//p' "$PROPS" | sed 's#\\##g')
[ -n "$URL" ] || { echo "distributionUrl missing in $PROPS" >&2; exit 1; }
NAME=$(basename "$URL" .zip)
BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/bootstrap"
DIR="$BASE/$NAME"
GRADLE_BIN=$(ls "$DIR"/gradle-*/bin/gradle 2>/dev/null | head -n 1 || true)

if [ -z "$GRADLE_BIN" ]; then
  echo "Bootstrapping $NAME from $URL"
  mkdir -p "$DIR"
  TMP="$DIR/dist.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$URL" -o "$TMP"
  else
    wget -q "$URL" -O "$TMP"
  fi
  unzip -q -o "$TMP" -d "$DIR"
  rm -f "$TMP"
  GRADLE_BIN=$(ls "$DIR"/gradle-*/bin/gradle | head -n 1)
fi

exec "$GRADLE_BIN" "$@"
