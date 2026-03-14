#!/usr/bin/env bash
set -euo pipefail

APP_HOME="${APP_HOME:-/opt/finventory}"
APP_REPO_DIR="${APP_REPO_DIR:-$APP_HOME/app}"
BACKEND_ARTIFACT_DIR="${BACKEND_ARTIFACT_DIR:-$APP_HOME/backend}"
APP_USER="${APP_USER:-finventory}"
APP_GROUP="${APP_GROUP:-finventory}"

GIT_REMOTE="${GIT_REMOTE:-origin}"
GIT_BRANCH="${GIT_BRANCH:-main}"

if [[ "$(id -u)" -ne 0 ]]; then
  echo "Run as root (sudo)."
  exit 1
fi

if [[ ! -d "$APP_REPO_DIR/.git" ]]; then
  echo "Repo not found at $APP_REPO_DIR"
  exit 1
fi

install -d -m 0750 -o "$APP_USER" -g "$APP_GROUP" "$APP_HOME"
chown -R "$APP_USER":"$APP_GROUP" "$APP_REPO_DIR"

sudo -u "$APP_USER" -H bash -lc "cd '$APP_REPO_DIR' && git fetch '$GIT_REMOTE' '$GIT_BRANCH' && git reset --hard '$GIT_REMOTE/$GIT_BRANCH'"

if [[ -f "$APP_REPO_DIR/deploy/systemd/finventory-backend.service" ]]; then
  install -m 0644 "$APP_REPO_DIR/deploy/systemd/finventory-backend.service" /etc/systemd/system/finventory-backend.service
fi
if [[ -f "$APP_REPO_DIR/deploy/systemd/finventory-frontend.service" ]]; then
  install -m 0644 "$APP_REPO_DIR/deploy/systemd/finventory-frontend.service" /etc/systemd/system/finventory-frontend.service
fi
if [[ -f "$APP_REPO_DIR/deploy/nginx/finventory.conf" ]] && [[ ! -f /etc/nginx/sites-available/finventory ]]; then
  install -m 0644 "$APP_REPO_DIR/deploy/nginx/finventory.conf" /etc/nginx/sites-available/finventory
  ln -sf /etc/nginx/sites-available/finventory /etc/nginx/sites-enabled/finventory
  rm -f /etc/nginx/sites-enabled/default
fi

sudo -u "$APP_USER" -H bash -lc "cd '$APP_REPO_DIR/backend' && mvn -DskipTests clean package"

JAR_PATH="$(ls -1 "$APP_REPO_DIR"/backend/target/*SNAPSHOT.jar | head -n 1)"
install -d "$BACKEND_ARTIFACT_DIR"
install -m 0644 "$JAR_PATH" "$BACKEND_ARTIFACT_DIR/finventory-backend.jar"
chown -R finventory:finventory "$BACKEND_ARTIFACT_DIR"

sudo -u "$APP_USER" -H bash -lc "cd '$APP_REPO_DIR/frontend' && npm ci && npm run build"

systemctl daemon-reload
nginx -t && systemctl reload nginx || true
systemctl restart finventory-backend
systemctl restart finventory-frontend

systemctl --no-pager --full status finventory-backend || true
systemctl --no-pager --full status finventory-frontend || true
