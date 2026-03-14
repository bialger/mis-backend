#!/usr/bin/env bash
# Останавливает и удаляет ранее установленный systemd-сервис приложения mis.
# Запускать на сервере перед переходом на деплой через Docker (или при ручном снятии с деплоя).

set -euo pipefail

SERVICE_NAME="${SERVICE_NAME:-mis}"
APP_DIR="${APP_DIR:-/opt/mis}"
SUDO_PASSWORD="${SUDO_PASSWORD:-}"

run_as_root() {
  if [ "$EUID" -eq 0 ]; then
    "$@"
    return
  fi
  if ! command -v sudo >/dev/null 2>&1; then
    echo "sudo is required."
    exit 1
  fi
  if [ -n "$SUDO_PASSWORD" ]; then
    printf '%s\n' "$SUDO_PASSWORD" | sudo -S "$@"
  else
    sudo "$@"
  fi
}

echo "Stopping and removing systemd service: ${SERVICE_NAME}..."

run_as_root systemctl stop "${SERVICE_NAME}.service" 2>/dev/null || true
run_as_root systemctl disable "${SERVICE_NAME}.service" 2>/dev/null || true
run_as_root pkill -f "${APP_DIR}/app.jar" 2>/dev/null || true
sleep 2

run_as_root rm -f "/etc/systemd/system/${SERVICE_NAME}.service"
run_as_root systemctl daemon-reload

echo "Service '${SERVICE_NAME}' stopped and unit file removed."

# Опционально: удалить файлы приложения в /opt/mis (раскомментируйте при необходимости)
# run_as_root rm -rf "$APP_DIR"
# echo "Application directory $APP_DIR removed."

echo "Done."
