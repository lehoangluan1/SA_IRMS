#!/usr/bin/env sh
set -u

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
DIAG_DIR="$PROJECT_ROOT/build-diagnostics"
ATTEMPTS_LOG="$DIAG_DIR/mvn-attempts.log"
DUMP_FILE="$DIAG_DIR/mvn-compile-dump.txt"
STRUCTURE_FILE="$DIAG_DIR/project-structure-dump.txt"
mkdir -p "$DIAG_DIR"
: > "$ATTEMPTS_LOG"
: > "$DUMP_FILE"

MAVEN_VERSION_TIMEOUT_SECONDS=${MAVEN_VERSION_TIMEOUT_SECONDS:-30}
MAVEN_COMPILE_TIMEOUT_SECONDS=${MAVEN_COMPILE_TIMEOUT_SECONDS:-300}
DOCKER_COMPILE_TIMEOUT_SECONDS=${DOCKER_COMPILE_TIMEOUT_SECONDS:-600}
NETWORK_CHECK_TIMEOUT_SECONDS=${NETWORK_CHECK_TIMEOUT_SECONDS:-20}

TIMEOUT_AVAILABLE=0
if command -v timeout >/dev/null 2>&1; then
  TIMEOUT_AVAILABLE=1
fi

log() {
  printf '%s\n' "$1" | tee -a "$ATTEMPTS_LOG"
}

append_dump() {
  printf '%s\n' "$1" >> "$DUMP_FILE"
}

run_capture() {
  timeout_seconds="$1"
  shift
  command_string="$*"
  if [ "$TIMEOUT_AVAILABLE" -eq 1 ]; then
    timeout "$timeout_seconds" sh -c "$command_string"
    return $?
  fi
  sh -c "$command_string"
  return $?
}

classify_result() {
  attempt_name="$1"
  exit_code="$2"
  output_file="$3"
  if [ "$exit_code" -eq 0 ]; then
    printf '%s' "COMPILE_SUCCESS"
    return 0
  fi

  if [ "$TIMEOUT_AVAILABLE" -eq 1 ] && [ "$exit_code" -eq 124 ]; then
    printf '%s' "TIMEOUT"
    return 0
  fi

  if grep -Eqi 'compilation failure|cannot find symbol|package .* does not exist|\[ERROR\] COMPILATION ERROR|Failed to execute goal .*compile' "$output_file"; then
    printf '%s' "CODE_COMPILE_FAILURE"
    return 0
  fi

  if grep -Eqi 'Could not resolve dependencies|Could not transfer artifact|Failed to read artifact descriptor|Temporary failure in name resolution|Name or service not known|Unknown host|No route to host|Connection timed out|Connection reset|transfer failed|Failed to fetch|Network is unreachable|status code 5[0-9][0-9]' "$output_file"; then
    if [ "$NETWORK_RESULT" = "NETWORK_UNAVAILABLE" ]; then
      printf '%s' "NETWORK_UNAVAILABLE"
      return 0
    fi
    printf '%s' "DEPENDENCY_RESOLUTION_FAILURE"
    return 0
  fi

  case "$attempt_name" in
    mvn)
      printf '%s' "UNKNOWN_FAILURE"
      ;;
    mvnw)
      printf '%s' "UNKNOWN_FAILURE"
      ;;
    docker-maven)
      printf '%s' "UNKNOWN_FAILURE"
      ;;
    *)
      printf '%s' "UNKNOWN_FAILURE"
      ;;
  esac
}

run_attempt() {
  attempt_name="$1"
  timeout_seconds="$2"
  shift 2
  command_string="$*"
  output_file="$DIAG_DIR/${attempt_name}.tmp.log"

  log "==> Attempt: $attempt_name"
  log "Command: $command_string"
  log "Timeout seconds: $timeout_seconds"

  run_capture "$timeout_seconds" "$command_string" > "$output_file" 2>&1
  exit_code=$?
  classification=$(classify_result "$attempt_name" "$exit_code" "$output_file")

  log "Exit code: $exit_code"
  log "Classification: $classification"
  cat "$output_file" | tee -a "$ATTEMPTS_LOG"

  append_dump ""
  append_dump "Attempt: $attempt_name"
  append_dump "Command: $command_string"
  append_dump "Timeout seconds: $timeout_seconds"
  append_dump "Exit code: $exit_code"
  append_dump "Classification: $classification"
  append_dump "Output:"
  cat "$output_file" >> "$DUMP_FILE"

  rm -f "$output_file"
  ATTEMPT_EXIT_CODE="$exit_code"
  ATTEMPT_CLASSIFICATION="$classification"
}

append_dump "IRMS Maven verification dump"
append_dump "Timestamp (UTC): $(date -u '+%Y-%m-%dT%H:%M:%SZ' 2>/dev/null || printf 'unknown')"
append_dump "Working directory: $PROJECT_ROOT"
append_dump ""
append_dump "Timeout configuration:"
append_dump "MAVEN_VERSION_TIMEOUT_SECONDS=$MAVEN_VERSION_TIMEOUT_SECONDS"
append_dump "MAVEN_COMPILE_TIMEOUT_SECONDS=$MAVEN_COMPILE_TIMEOUT_SECONDS"
append_dump "DOCKER_COMPILE_TIMEOUT_SECONDS=$DOCKER_COMPILE_TIMEOUT_SECONDS"
append_dump "NETWORK_CHECK_TIMEOUT_SECONDS=$NETWORK_CHECK_TIMEOUT_SECONDS"
append_dump "TIMEOUT_AVAILABLE=$TIMEOUT_AVAILABLE"
append_dump ""
append_dump "Java version:"
(java -version 2>&1 || printf 'java not available\n') >> "$DUMP_FILE"
append_dump ""
append_dump "Maven version:"
if command -v mvn >/dev/null 2>&1; then
  if [ "$TIMEOUT_AVAILABLE" -eq 1 ]; then
    timeout "$MAVEN_VERSION_TIMEOUT_SECONDS" mvn -v >> "$DUMP_FILE" 2>&1 || printf 'mvn -v timed out or failed\n' >> "$DUMP_FILE"
  else
    printf 'warning: timeout command unavailable; mvn -v not timeout-protected\n' >> "$DUMP_FILE"
    mvn -v >> "$DUMP_FILE" 2>&1 || printf 'mvn -v failed\n' >> "$DUMP_FILE"
  fi
else
  printf 'mvn not available\n' >> "$DUMP_FILE"
fi
append_dump ""
append_dump "Maven wrapper availability:"
if [ -x "$PROJECT_ROOT/mvnw" ]; then
  printf 'mvnw executable: yes\n' >> "$DUMP_FILE"
else
  printf 'mvnw executable: no\n' >> "$DUMP_FILE"
fi
append_dump ""
append_dump "Docker availability:"
if command -v docker >/dev/null 2>&1; then
  docker --version >> "$DUMP_FILE" 2>&1
else
  printf 'docker not available\n' >> "$DUMP_FILE"
fi
append_dump ""
append_dump "Network/DNS check:"
NETWORK_RESULT="UNKNOWN_FAILURE"
if command -v curl >/dev/null 2>&1; then
  if [ "$TIMEOUT_AVAILABLE" -eq 1 ]; then
    timeout "$NETWORK_CHECK_TIMEOUT_SECONDS" curl -sS -I --max-time "$NETWORK_CHECK_TIMEOUT_SECONDS" https://repo.maven.apache.org/maven2/ >> "$DUMP_FILE" 2>&1
    network_exit=$?
  else
    printf 'warning: timeout command unavailable; curl check relies on --max-time only\n' >> "$DUMP_FILE"
    curl -sS -I --max-time "$NETWORK_CHECK_TIMEOUT_SECONDS" https://repo.maven.apache.org/maven2/ >> "$DUMP_FILE" 2>&1
    network_exit=$?
  fi
  if [ "$network_exit" -eq 0 ]; then
    NETWORK_RESULT="COMPILE_SUCCESS"
  elif [ "$TIMEOUT_AVAILABLE" -eq 1 ] && [ "$network_exit" -eq 124 ]; then
    NETWORK_RESULT="TIMEOUT"
  else
    NETWORK_RESULT="NETWORK_UNAVAILABLE"
  fi
else
  printf 'curl not available; network check skipped\n' >> "$DUMP_FILE"
  NETWORK_RESULT="UNKNOWN_FAILURE"
fi
append_dump "Network classification: $NETWORK_RESULT"
append_dump ""
append_dump "Selected environment values:"
append_dump "SERVER_PORT=${SERVER_PORT-}"
append_dump "SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE-}"
append_dump "IRMS_RUNTIME_MODE=${IRMS_RUNTIME_MODE-}"
append_dump "JAVA_HOME=${JAVA_HOME-}"
append_dump ""
append_dump "pom.xml dependency summary:"
(grep -n '<groupId>\|<artifactId>\|<version>' "$PROJECT_ROOT/pom.xml" 2>&1 || printf 'pom.xml summary unavailable\n') >> "$DUMP_FILE"

(
  cd "$PROJECT_ROOT" || exit 1
  {
    printf 'Project structure dump\n'
    printf 'Generated at: %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ' 2>/dev/null || printf 'unknown')"
    printf 'Root: %s\n\n' "$PROJECT_ROOT"
    find . | sort
  } > "$STRUCTURE_FILE"
)

FINAL_RESULT="UNKNOWN_FAILURE"
FINAL_REASON=""
SUCCESS=0

if command -v mvn >/dev/null 2>&1; then
  run_attempt "mvn" "$MAVEN_COMPILE_TIMEOUT_SECONDS" "cd '$PROJECT_ROOT' && mvn -DskipTests compile"
  if [ "$ATTEMPT_EXIT_CODE" -eq 0 ]; then
    SUCCESS=1
    FINAL_RESULT="COMPILE_SUCCESS"
    FINAL_REASON="mvn compile succeeded"
  else
    FINAL_RESULT="$ATTEMPT_CLASSIFICATION"
    FINAL_REASON="mvn compile failed"
  fi
else
  log "mvn not available"
  FINAL_RESULT="MAVEN_NOT_AVAILABLE"
  FINAL_REASON="mvn command not available"
fi

if [ "$SUCCESS" -ne 1 ]; then
  if [ -x "$PROJECT_ROOT/mvnw" ]; then
    run_attempt "mvnw" "$MAVEN_COMPILE_TIMEOUT_SECONDS" "cd '$PROJECT_ROOT' && ./mvnw -DskipTests compile"
    if [ "$ATTEMPT_EXIT_CODE" -eq 0 ]; then
      SUCCESS=1
      FINAL_RESULT="COMPILE_SUCCESS"
      FINAL_REASON="mvnw compile succeeded"
    else
      FINAL_RESULT="$ATTEMPT_CLASSIFICATION"
      FINAL_REASON="mvnw compile failed with $ATTEMPT_CLASSIFICATION"
    fi
  else
    log "mvnw not executable or unavailable"
    if [ "$FINAL_RESULT" = "MAVEN_NOT_AVAILABLE" ] || [ "$FINAL_RESULT" = "UNKNOWN_FAILURE" ]; then
      FINAL_RESULT="MAVEN_WRAPPER_UNAVAILABLE"
      FINAL_REASON="mvnw not executable or unavailable"
    fi
  fi
fi

if [ "$SUCCESS" -ne 1 ]; then
  if command -v docker >/dev/null 2>&1; then
    run_attempt "docker-maven" "$DOCKER_COMPILE_TIMEOUT_SECONDS" "cd '$PROJECT_ROOT' && docker run --rm -v '$PROJECT_ROOT:/workspace' -w /workspace maven:3.9.9-eclipse-temurin-17 mvn -DskipTests compile"
    if [ "$ATTEMPT_EXIT_CODE" -eq 0 ]; then
      SUCCESS=1
      FINAL_RESULT="COMPILE_SUCCESS"
      FINAL_REASON="dockerized maven compile succeeded"
    else
      FINAL_RESULT="$ATTEMPT_CLASSIFICATION"
      FINAL_REASON="dockerized maven compile failed with $ATTEMPT_CLASSIFICATION"
    fi
  else
    log "docker not available"
    if [ "$SUCCESS" -ne 1 ] && { [ "$FINAL_RESULT" = "MAVEN_NOT_AVAILABLE" ] || [ "$FINAL_RESULT" = "MAVEN_WRAPPER_UNAVAILABLE" ]; }; then
      FINAL_RESULT="DOCKER_NOT_AVAILABLE"
      FINAL_REASON="docker not available"
    fi
  fi
fi

append_dump ""
append_dump "Final result: $FINAL_RESULT"
append_dump "Final reason: $FINAL_REASON"

case "$FINAL_RESULT" in
  COMPILE_SUCCESS)
    append_dump "Next recommended action: package the application artifact or run integration startup checks."
    exit 0
    ;;
  CODE_COMPILE_FAILURE)
    append_dump "Next recommended action: fix source-level compilation errors shown above, then rerun scripts/verify-mvn.sh."
    exit 1
    ;;
  DEPENDENCY_RESOLUTION_FAILURE|NETWORK_UNAVAILABLE)
    append_dump "Next recommended action: restore Maven repository/network access, then rerun scripts/verify-mvn.sh."
    exit 1
    ;;
  MAVEN_NOT_AVAILABLE|MAVEN_WRAPPER_UNAVAILABLE|DOCKER_NOT_AVAILABLE)
    append_dump "Next recommended action: install or enable at least one supported build path (mvn, mvnw, or docker) and rerun scripts/verify-mvn.sh."
    exit 1
    ;;
  TIMEOUT)
    append_dump "Next recommended action: increase timeout environment variables or restore network/build responsiveness, then rerun scripts/verify-mvn.sh."
    exit 1
    ;;
  *)
    append_dump "Next recommended action: inspect build-diagnostics/mvn-attempts.log for the exact failure and rerun scripts/verify-mvn.sh after correcting the environment or source issue."
    exit 1
    ;;
esac
