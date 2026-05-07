#!/bin/sh
set -eu

if [ ! -d node_modules ] || [ ! -d node_modules/vuetify ] || [ ! -d node_modules/vitest ]; then
  echo "Installing frontend dependencies..."
  npm install
fi

exec npm run dev -- --host 0.0.0.0
