"""Choose available loopback host ports on first launch, preserving existing .env files."""
import os
import socket
from pathlib import Path

chosen = {}
for key, preferred in (("ANALYZER_UI_PORT", 33479), ("ANALYZER_API_PORT", 38479), ("ANALYZER_DB_PORT", 35479)):
    start = int(os.environ.get(key, preferred))
    if not 1024 <= start <= 64535:
        raise SystemExit(f"{key} must be between 1024 and 64535")
    for port in range(start, start + 1000):
        if port in chosen.values():
            continue
        try:
            with socket.socket() as probe:
                probe.bind(("127.0.0.1", port))
        except OSError:
            continue
        chosen[key] = port
        break
    else:
        raise SystemExit(f"Could not find a free port for {key}")

# Exclusive create: never replace a user's existing port configuration.
with Path(".env").open("x") as env:
    env.write("".join(f"{key}={value}\n" for key, value in chosen.items()))
for key, value in chosen.items():
    print(f"{key}={value}")
