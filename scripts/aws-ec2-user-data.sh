#!/bin/bash
set -euo pipefail

exec > >(tee /var/log/deploylab-bootstrap.log | logger -t deploylab-user-data -s 2>/dev/console) 2>&1

dnf update -y
dnf install -y docker git openssl
systemctl enable --now docker

# The free-tier t3.micro has 1 GiB RAM. Swap keeps the one-time Maven image
# build from being killed while leaving the final API and database unaffected.
fallocate -l 1G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile swap swap defaults 0 0' >> /etc/fstab

install -d -m 0755 /opt/deploylab
git clone --depth 1 https://github.com/tomlaura-bit/deploylab-backend.git /opt/deploylab/source

DB_PASSWORD="$(openssl rand -hex 24)"
JWT_SECRET="$(openssl rand -hex 48)"

docker network create deploylab-network
docker volume create deploylab-postgres-data

docker run -d \
  --name deploylab-db \
  --restart unless-stopped \
  --network deploylab-network \
  --health-cmd='pg_isready -U deploylab -d deploylab' \
  --health-interval=5s \
  --health-timeout=5s \
  --health-retries=30 \
  -v deploylab-postgres-data:/var/lib/postgresql/data \
  -e POSTGRES_DB=deploylab \
  -e POSTGRES_USER=deploylab \
  -e POSTGRES_PASSWORD="$DB_PASSWORD" \
  postgres:17

until [ "$(docker inspect --format='{{.State.Health.Status}}' deploylab-db)" = "healthy" ]; do
  sleep 3
done

docker build -t deploylab-api:latest /opt/deploylab/source

docker run -d \
  --name deploylab-api \
  --restart unless-stopped \
  --network deploylab-network \
  -p 80:8080 \
  -e DB_URL='jdbc:postgresql://deploylab-db:5432/deploylab' \
  -e DB_USER=deploylab \
  -e DB_PASSWORD="$DB_PASSWORD" \
  -e JWT_SECRET="$JWT_SECRET" \
  -e CORS_ORIGIN='*' \
  -e EXTRAS_ENABLED=true \
  deploylab-api:latest
