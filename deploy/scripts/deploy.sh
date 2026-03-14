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
  echo "Repo not found at $APP_REPO_DIR. Clone the repo there before deploying."
  exit 1
fi
if [[ ! -d "$APP_REPO_DIR/backend" ]] || [[ ! -d "$APP_REPO_DIR/frontend" ]]; then
  echo "Repo at $APP_REPO_DIR is missing backend/ or frontend/ directories."
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

if [[ -f "$APP_REPO_DIR/deploy/nginx/finventory.conf" ]]; then
  if [[ ! -f /etc/nginx/sites-available/finventory ]]; then
    install -m 0644 "$APP_REPO_DIR/deploy/nginx/finventory.conf" /etc/nginx/sites-available/finventory
  fi

  if grep -qE "location \\^~ /api/\\s*\\{" /etc/nginx/sites-available/finventory; then
    sed -i "s#location \\^~ /api/#location ^~ /api/v1/#" /etc/nginx/sites-available/finventory
  fi

  if ! grep -qE "location \\^~ /api/reports/" /etc/nginx/sites-available/finventory; then
    sed -i '/^}$/i\
\
  location ^~ /api/reports/ {\
    proxy_pass http://127.0.0.1:8080;\
    proxy_http_version 1.1;\
    proxy_set_header Host $host;\
    proxy_set_header X-Real-IP $remote_addr;\
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\
    proxy_set_header X-Forwarded-Proto $scheme;\
  }\
' /etc/nginx/sites-available/finventory
  fi

  if ! grep -qE "location \\^~ /api/auth/" /etc/nginx/sites-available/finventory; then
    sed -i '/^}$/i\
\
  location ^~ /api/auth/ {\
    proxy_pass http://127.0.0.1:3000;\
    proxy_http_version 1.1;\
    proxy_set_header Host $host;\
    proxy_set_header X-Real-IP $remote_addr;\
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\
    proxy_set_header X-Forwarded-Proto $scheme;\
  }\
' /etc/nginx/sites-available/finventory
  fi

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

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required for deploy health checks. Install it (apt-get install -y curl) and re-run."
  exit 1
fi

backend_url="http://127.0.0.1:8080/health"
frontend_url="http://127.0.0.1:3000/"

backend_ok=0
for _ in $(seq 1 30); do
  code="$(curl -sS -o /dev/null -w "%{http_code}" --max-time 2 "$backend_url" || echo 000)"
  if [[ "$code" == "200" ]]; then
    backend_ok=1
    break
  fi
  sleep 2
done

frontend_ok=0
for _ in $(seq 1 30); do
  code="$(curl -sS -o /dev/null -w "%{http_code}" --max-time 2 "$frontend_url" || echo 000)"
  case "$code" in
    2*|3*) frontend_ok=1; break ;;
  esac
  sleep 2
done

if [[ "$backend_ok" != "1" ]]; then
  echo "Backend failed health check: $backend_url"
  systemctl --no-pager --full status finventory-backend || true
  journalctl -u finventory-backend -n 200 --no-pager || true
  exit 1
fi

if [[ "$frontend_ok" != "1" ]]; then
  echo "Frontend failed health check: $frontend_url"
  systemctl --no-pager --full status finventory-frontend || true
  journalctl -u finventory-frontend -n 200 --no-pager || true
  exit 1
fi

systemctl --no-pager --full status finventory-backend || true
systemctl --no-pager --full status finventory-frontend || true
