#!/usr/bin/env bash

set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"

cd "$DIR"

exec java -jar build/libs/confession-discord-bot.jar