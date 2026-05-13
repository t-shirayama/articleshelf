#!/usr/bin/env bash
set -euo pipefail

config_file="${1:-render.yaml}"

if [[ ! -f "$config_file" ]]; then
  echo "Missing Render blueprint: $config_file" >&2
  exit 1
fi

assert_key_present() {
  local key="$1"
  if ! grep -Fq -- "key: ${key}" "$config_file"; then
    echo "Missing Render env var definition: ${key}" >&2
    exit 1
  fi
}

assert_key_value() {
  local key="$1"
  local expected="$2"
  if ! awk -v key="$key" -v expected="$expected" '
    $0 ~ "key: " key "$" { in_block=1; next }
    in_block && $1 == "value:" {
      value = $2
      gsub(/"/, "", value)
      if (value == expected) found=1
      exit
    }
    in_block && $1 == "-" { exit }
    END { exit found ? 0 : 1 }
  ' "$config_file"; then
    echo "Expected ${key}=${expected} in ${config_file}" >&2
    exit 1
  fi
}

assert_key_present "SPRING_PROFILES_ACTIVE"
assert_key_present "FRONTEND_ORIGIN"
assert_key_present "SPRING_DATASOURCE_URL"
assert_key_present "SPRING_DATASOURCE_USERNAME"
assert_key_present "SPRING_DATASOURCE_PASSWORD"
assert_key_present "JWT_ACCESS_SECRET"
assert_key_present "AUTH_REFRESH_TOKEN_HASH_SECRET"

assert_key_value "SPRING_PROFILES_ACTIVE" "prod"
assert_key_value "AUTH_CSRF_ENABLED" "true"
assert_key_value "AUTH_COOKIE_SECURE" "true"
assert_key_value "AUTH_COOKIE_SAME_SITE" "None"
assert_key_value "ARTICLESHELF_INITIAL_USER_ENABLED" "false"
assert_key_value "ARTICLESHELF_OGP_REQUIRE_PROXY_IN_PROD" "false"

if ! grep -Fq "healthCheckPath: /actuator/health" "$config_file"; then
  echo "Render blueprint must keep /actuator/health as the health check path" >&2
  exit 1
fi

echo "Render backend production profile config looks valid."
