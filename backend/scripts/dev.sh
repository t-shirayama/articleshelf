#!/bin/sh
set -eu

STATE_FILE=/tmp/readstack-source-state
PREVIOUS_STATE_FILE=/tmp/readstack-source-state.previous
POLL_INTERVAL="${HOT_RELOAD_POLL_INTERVAL:-2}"
JVM_ARGUMENTS="${SPRING_BOOT_RUN_JVM_ARGUMENTS:--Dspring.devtools.restart.poll-interval=2s -Dspring.devtools.restart.quiet-period=1s}"

snapshot_sources() {
  {
    find src/main/java src/main/resources -type f 2>/dev/null || true
    [ -f pom.xml ] && printf '%s\n' pom.xml
  } | sort | while IFS= read -r file; do
    [ -f "$file" ] && stat -c '%Y %s %n' "$file"
  done
}

compile_when_sources_change() {
  while true; do
    snapshot_sources > "$STATE_FILE"

    if [ ! -f "$PREVIOUS_STATE_FILE" ]; then
      cp "$STATE_FILE" "$PREVIOUS_STATE_FILE"
    elif ! cmp -s "$STATE_FILE" "$PREVIOUS_STATE_FILE"; then
      cp "$STATE_FILE" "$PREVIOUS_STATE_FILE"
      mvn -q -DskipTests compile || true
    fi

    sleep "$POLL_INTERVAL"
  done
}

mvn -q -DskipTests compile
compile_when_sources_change &
mvn spring-boot:run -Dspring-boot.run.jvmArguments="$JVM_ARGUMENTS"
