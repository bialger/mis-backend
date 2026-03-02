#!/usr/bin/env bash
set -euo pipefail

APP_NAME="${APP_NAME:-mis}"
SERVICE_NAME="${SERVICE_NAME:-mis}"
APP_USER="${APP_USER:-$USER}"
APP_DIR="${APP_DIR:-/opt/mis}"
ARTIFACT_JAR="${ARTIFACT_JAR:-app.jar}"
SUDO_PASSWORD="${SUDO_PASSWORD:-}"

if ! command -v java >/dev/null 2>&1; then
  echo "Java is not installed. Install JRE/JDK 21+ before deployment."
  exit 1
fi

if ! command -v systemctl >/dev/null 2>&1; then
  echo "systemd is required for auto-start on boot."
  exit 1
fi

if [ ! -f "$ARTIFACT_JAR" ]; then
  echo "Artifact not found: $ARTIFACT_JAR"
  exit 1
fi

run_as_root() {
  if [ "$EUID" -eq 0 ]; then
    "$@"
    return
  fi

  if ! command -v sudo >/dev/null 2>&1; then
    echo "sudo is required for service installation."
    exit 1
  fi

  if [ -n "$SUDO_PASSWORD" ]; then
    printf '%s\n' "$SUDO_PASSWORD" | sudo -S "$@"
  else
    sudo "$@"
  fi
}

echo "Deploying $APP_NAME to $APP_DIR with service $SERVICE_NAME..."

run_as_root mkdir -p "$APP_DIR"
run_as_root cp "$ARTIFACT_JAR" "$APP_DIR/app.jar"
run_as_root chown -R "$APP_USER:$APP_USER" "$APP_DIR"

SERVICE_FILE="/tmp/${SERVICE_NAME}.service"
cat > "$SERVICE_FILE" <<EOF
[Unit]
Description=${APP_NAME} Micronaut Service
After=network.target

[Service]
Type=simple
User=${APP_USER}
WorkingDirectory=${APP_DIR}
ExecStart=/usr/bin/env java -jar ${APP_DIR}/app.jar
SuccessExitStatus=143
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

run_as_root cp "$SERVICE_FILE" "/etc/systemd/system/${SERVICE_NAME}.service"
rm -f "$SERVICE_FILE"

run_as_root systemctl daemon-reload
run_as_root systemctl enable "${SERVICE_NAME}.service"
run_as_root systemctl restart "${SERVICE_NAME}.service"
run_as_root systemctl --no-pager --full status "${SERVICE_NAME}.service" | head -n 30

echo "Deployment complete. Service '${SERVICE_NAME}' is enabled for autostart."
