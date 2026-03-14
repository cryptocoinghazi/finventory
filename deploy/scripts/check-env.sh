#!/usr/bin/env bash
set -euo pipefail

export DEBIAN_FRONTEND=noninteractive

require_root() {
  if [[ "$(id -u)" -ne 0 ]]; then
    echo "Run as root (sudo)."
    exit 1
  fi
}

ensure_pkg() {
  local pkg="$1"
  if dpkg -s "$pkg" >/dev/null 2>&1; then
    return 0
  fi
  apt-get update
  apt-get install -y --no-install-recommends "$pkg"
}

ensure_node_20() {
  if command -v node >/dev/null 2>&1; then
    local major
    major="$(node -v | sed 's/^v//' | cut -d. -f1)"
    if [[ "$major" == "20" ]]; then
      return 0
    fi
  fi

  ensure_pkg ca-certificates
  ensure_pkg curl
  ensure_pkg gnupg

  mkdir -p /etc/apt/keyrings
  if [[ ! -f /etc/apt/keyrings/nodesource.gpg ]]; then
    curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg
  fi

  local node_major=20
  echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_${node_major}.x nodistro main" > /etc/apt/sources.list.d/nodesource.list
  apt-get update
  apt-get install -y nodejs
}

ensure_java_21() {
  if command -v java >/dev/null 2>&1; then
    local major
    major="$(java -version 2>&1 | head -n 1 | sed -E 's/.*\"([0-9]+).*/\1/')"
    if [[ "$major" == "21" ]]; then
      return 0
    fi
  fi

  ensure_pkg ca-certificates
  ensure_pkg curl
  ensure_pkg gnupg

  mkdir -p /etc/apt/keyrings
  if [[ ! -f /etc/apt/keyrings/adoptium.gpg ]]; then
    curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
  fi

  if [[ ! -f /etc/apt/sources.list.d/adoptium.list ]]; then
    echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(. /etc/os-release && echo ${VERSION_CODENAME}) main" > /etc/apt/sources.list.d/adoptium.list
  fi

  apt-get update
  apt-get install -y temurin-21-jdk
}

report_versions() {
  echo "Versions:"
  command -v git >/dev/null 2>&1 && git --version || true
  command -v java >/dev/null 2>&1 && java -version || true
  command -v mvn >/dev/null 2>&1 && mvn -v || true
  command -v node >/dev/null 2>&1 && node -v || true
  command -v npm >/dev/null 2>&1 && npm -v || true
  command -v psql >/dev/null 2>&1 && psql --version || true
  command -v nginx >/dev/null 2>&1 && nginx -v || true
  command -v ufw >/dev/null 2>&1 && ufw version || true
  command -v fail2ban-client >/dev/null 2>&1 && fail2ban-client --version || true
}

require_root

ensure_pkg git
ensure_java_21
ensure_pkg maven
ensure_node_20
ensure_pkg postgresql
ensure_pkg postgresql-contrib
ensure_pkg nginx
ensure_pkg ufw
ensure_pkg fail2ban

report_versions
