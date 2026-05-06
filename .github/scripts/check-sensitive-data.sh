#!/usr/bin/env bash
set -euo pipefail

tracked_secret_files="$(git ls-files | grep -E '(^|/)\.env($|\.)|\.pem$|\.key$|id_rsa$' || true)"

if [[ -n "${tracked_secret_files}" ]]; then
  echo "Tracked secret-like files found:"
  echo "${tracked_secret_files}"
  exit 1
fi

pattern='([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}|/Users/|[A-Za-z]:/Users/|BEGIN RSA PRIVATE KEY|BEGIN OPENSSH PRIVATE KEY|AKIA[0-9A-Z]{16}|ghp_[A-Za-z0-9]{36}|xox[baprs]-[A-Za-z0-9-]+|AIza[0-9A-Za-z_-]{35}|sk-[A-Za-z0-9]{20,})'

if git grep -nI -E "${pattern}" -- \
  . \
  ':(exclude)docs/designs/*' \
  ':(exclude)frontend/package-lock.json'
then
  echo
  echo "Potential sensitive data found in tracked files."
  exit 1
fi

echo "No sensitive data patterns found in tracked files."
