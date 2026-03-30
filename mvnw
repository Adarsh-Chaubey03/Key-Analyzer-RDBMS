#!/bin/sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_JAR="$BASE_DIR/.mvn/wrapper/maven-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Error: could not find $WRAPPER_JAR" >&2
  exit 1
fi

if [ -n "${JAVA_HOME:-}" ]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
else
  JAVA_CMD="java"
fi

exec "$JAVA_CMD" ${JVM_CONFIG_MAVEN_PROPS:-} ${MAVEN_OPTS:-} ${MAVEN_DEBUG_OPTS:-} \
  -classpath "$WRAPPER_JAR" \
  "-Dmaven.multiModuleProjectDirectory=$BASE_DIR" \
  org.apache.maven.wrapper.MavenWrapperMain "$@"
