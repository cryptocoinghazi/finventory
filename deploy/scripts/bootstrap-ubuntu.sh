#!/usr/bin/env bash
set -euo pipefail

APP_USER="${APP_USER:-finventory}"
APP_GROUP="${APP_GROUP:-finventory}"
APP_HOME="${APP_HOME:-/opt/finventory}"
APP_REPO_DIR="${APP_REPO_DIR:-$APP_HOME/app}"
BACKEND_DIR="${BACKEND_DIR:-$APP_HOME/backend}"
ENV_DIR="${ENV_DIR:-/etc/finventory}"
UPLOADS_DIR="${UPLOADS_DIR:-/var/lib/finventory/uploads}"

DB_NAME="${FINVENTORY_DB_NAME:-finventory}"
DB_USER="${FINVENTORY_DB_USER:-finventory}"
DB_PASSWORD="${FINVENTORY_DB_PASSWORD:-StrongPassword}"

DOMAIN_NAME="${FINVENTORY_DOMAIN_NAME:-}"
SSL_EMAIL="${FINVENTORY_SSL_EMAIL:-}"
GIT_URL="${FINVENTORY_GIT_URL:-}"
GIT_BRANCH="${FINVENTORY_GIT_BRANCH:-main}"

if [[ "$(id -u)" -ne 0 ]]; then
  echo "Run as root (sudo)."
  exit 1
fi

export DEBIAN_FRONTEND=noninteractive

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
bash "$SCRIPT_DIR/check-env.sh"

groupadd --force "$APP_GROUP"
if ! id "$APP_USER" >/dev/null 2>&1; then
  useradd --system --create-home --home-dir "$APP_HOME" --shell /usr/sbin/nologin --gid "$APP_GROUP" "$APP_USER"
fi

mkdir -p "$APP_HOME" "$APP_REPO_DIR" "$BACKEND_DIR" "$ENV_DIR"
chown -R "$APP_USER":"$APP_GROUP" "$APP_HOME"
chmod 750 "$APP_HOME"
chmod 750 "$ENV_DIR"

mkdir -p /var/www/html
chown www-data:www-data /var/www/html
chmod 755 /var/www/html

mkdir -p "$UPLOADS_DIR"
chown -R "$APP_USER":"$APP_GROUP" "$(dirname "$UPLOADS_DIR")"
chmod 750 "$(dirname "$UPLOADS_DIR")"

if [[ -n "$GIT_URL" ]]; then
  if [[ ! -d "$APP_REPO_DIR/.git" ]]; then
    rm -rf "$APP_REPO_DIR"
    install -d -m 0750 -o "$APP_USER" -g "$APP_GROUP" "$APP_REPO_DIR"
    sudo -u "$APP_USER" -H git clone --branch "$GIT_BRANCH" "$GIT_URL" "$APP_REPO_DIR"
  fi
fi

systemctl enable --now postgresql

sudo -u postgres psql -v ON_ERROR_STOP=1 <<SQL
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '${DB_USER}') THEN
    CREATE ROLE ${DB_USER} LOGIN PASSWORD '${DB_PASSWORD}';
  ELSE
    ALTER ROLE ${DB_USER} WITH LOGIN PASSWORD '${DB_PASSWORD}';
  END IF;
END
\$\$;

DO \$\$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_database WHERE datname = '${DB_NAME}') THEN
    CREATE DATABASE ${DB_NAME} OWNER ${DB_USER};
  END IF;
END
\$\$;
SQL

PG_CONF_DIR="$(sudo -u postgres psql -tAc "SHOW config_file" | xargs dirname)"
PG_HBA_DIR="$(sudo -u postgres psql -tAc "SHOW hba_file" | xargs dirname)"

sed -i "s/^#\\?listen_addresses\\s*=.*/listen_addresses = '127.0.0.1'/" "$PG_CONF_DIR/postgresql.conf"

if ! grep -qE "^host\\s+${DB_NAME}\\s+${DB_USER}\\s+127\\.0\\.0\\.1/32\\s+scram-sha-256" "$PG_HBA_DIR/pg_hba.conf"; then
  echo "host ${DB_NAME} ${DB_USER} 127.0.0.1/32 scram-sha-256" >> "$PG_HBA_DIR/pg_hba.conf"
fi

systemctl restart postgresql

ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable

systemctl enable --now fail2ban

if [[ -f "$APP_REPO_DIR/deploy/systemd/finventory-backend.service" ]]; then
  install -m 0644 "$APP_REPO_DIR/deploy/systemd/finventory-backend.service" /etc/systemd/system/finventory-backend.service
fi
if [[ -f "$APP_REPO_DIR/deploy/systemd/finventory-frontend.service" ]]; then
  install -m 0644 "$APP_REPO_DIR/deploy/systemd/finventory-frontend.service" /etc/systemd/system/finventory-frontend.service
fi

if [[ -f "$APP_REPO_DIR/deploy/env/backend.env.example" ]] && [[ ! -f "$ENV_DIR/backend.env" ]]; then
  install -m 0640 "$APP_REPO_DIR/deploy/env/backend.env.example" "$ENV_DIR/backend.env"
  chown root:"$APP_GROUP" "$ENV_DIR/backend.env"
fi
if [[ -f "$APP_REPO_DIR/deploy/env/frontend.env.example" ]] && [[ ! -f "$ENV_DIR/frontend.env" ]]; then
  install -m 0640 "$APP_REPO_DIR/deploy/env/frontend.env.example" "$ENV_DIR/frontend.env"
  chown root:"$APP_GROUP" "$ENV_DIR/frontend.env"
fi

if [[ -f "$APP_REPO_DIR/deploy/nginx/finventory.conf" ]]; then
  if [[ ! -f /etc/nginx/sites-available/finventory ]]; then
    install -m 0644 "$APP_REPO_DIR/deploy/nginx/finventory.conf" /etc/nginx/sites-available/finventory
    if [[ -n "$DOMAIN_NAME" ]]; then
      sed -i "s/server_name .*/server_name ${DOMAIN_NAME};/" /etc/nginx/sites-available/finventory
    else
      sed -i "s/server_name .*/server_name _;/" /etc/nginx/sites-available/finventory
    fi
    ln -sf /etc/nginx/sites-available/finventory /etc/nginx/sites-enabled/finventory
    rm -f /etc/nginx/sites-enabled/default
    nginx -t
    systemctl reload nginx
  fi
fi

if [[ -n "$DOMAIN_NAME" ]]; then
  if [[ -z "$SSL_EMAIL" ]]; then
    echo "FINVENTORY_SSL_EMAIL is required when FINVENTORY_DOMAIN_NAME is set."
    exit 1
  fi
  apt-get update
  apt-get install -y --no-install-recommends certbot python3-certbot-nginx
  certbot --nginx -d "$DOMAIN_NAME" --redirect --agree-tos -m "$SSL_EMAIL" --non-interactive
fi

systemctl daemon-reload
systemctl enable finventory-backend finventory-frontend || true

if [[ -n "$DOMAIN_NAME" ]]; then
  echo "Domain configured: ${DOMAIN_NAME}"
fi

echo "Bootstrap complete."
