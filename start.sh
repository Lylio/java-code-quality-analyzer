#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required. Start Docker Desktop or install Docker Engine with Compose." >&2
  exit 1
fi
docker compose version >/dev/null
docker info >/dev/null
if [[ ! -f .env ]]; then
  python3 scripts/select_ports.py
fi
docker compose up --build -d
address="$(docker compose port frontend 80)"
printf '\nDashboard: http://%s\nStartup logs: docker compose logs -f backend\n' "$address"
