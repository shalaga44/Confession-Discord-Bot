#!/usr/bin/env bash

set -euo pipefail

APP_DIR="${1:-}"
RUN_USER="${2:-}"
SERVICE_NAME="${3:-confession-discord-bot}"

if [[ -z "$APP_DIR" || -z "$RUN_USER" ]]; then
  echo "usage: $0 <app_dir> <run_user> [service_name]" >&2
  exit 1
fi

if [[ ! -d "$APP_DIR" ]]; then
  echo "app directory does not exist: $APP_DIR" >&2
  exit 1
fi

if [[ ! -f "$APP_DIR/deploy/confession-discord-bot.service" ]]; then
  echo "missing service template: $APP_DIR/deploy/confession-discord-bot.service" >&2
  exit 1
fi

if [[ ! -f "$APP_DIR/run.sh" ]]; then
  echo "missing run script: $APP_DIR/run.sh" >&2
  exit 1
fi

install -d -m 0755 /etc/systemd/system

TMP_FILE="$(mktemp)"
trap 'rm -f "$TMP_FILE"' EXIT

sed \
  -e "s|__APP_DIR__|$APP_DIR|g" \
  -e "s|__RUN_USER__|$RUN_USER|g" \
  "$APP_DIR/deploy/confession-discord-bot.service" > "$TMP_FILE"

install -m 0644 "$TMP_FILE" "/etc/systemd/system/${SERVICE_NAME}.service"

SERVICE_UNIT="${SERVICE_NAME}"

if [[ "$SERVICE_UNIT" != *.service ]]; then
  SERVICE_UNIT="${SERVICE_UNIT}.service"
fi

SUDOERS_FILE="/etc/sudoers.d/${SERVICE_NAME}-restart"
TMP_SUDOERS="$(mktemp)"

trap 'rm -f "$TMP_FILE" "$TMP_SUDOERS"' EXIT

cat > "$TMP_SUDOERS" <<EOF
${RUN_USER} ALL=(root) NOPASSWD: /bin/systemctl restart ${SERVICE_UNIT}, /usr/bin/systemctl restart ${SERVICE_UNIT}
EOF

chmod 0440 "$TMP_SUDOERS"

visudo -cf "$TMP_SUDOERS" >/dev/null

install -m 0440 "$TMP_SUDOERS" "$SUDOERS_FILE"

systemctl daemon-reload
systemctl enable "$SERVICE_NAME"