#!/bin/sh
set -eu

if [ ! -d node_modules ] || [ ! -d node_modules/vuetify ]; then
  echo "Installing frontend dependencies..."
  npm install
fi

exec npm run dev -- --host 0.0.0.0
