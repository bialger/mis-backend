#!/bin/bash
# Run Docker Compose with a locally built image (no pull from GHCR).
# Requires: Docker Desktop running, JDK 21.
set -e
cd "$(dirname "$0")/.."

echo "1. Building JAR..."
./gradlew shadowJar -x test

JAR=$(ls -1 build/libs/*-all.jar 2>/dev/null | head -1)
if [ -z "$JAR" ]; then
  echo "Error: JAR not found in build/libs/"
  exit 1
fi

echo "2. Building Docker image..."
mkdir -p deploy/docker-context
cp "$JAR" deploy/docker-context/app.jar
cp Dockerfile deploy/docker-context/
docker build -t mis-backend:local deploy/docker-context

echo "3. Creating .env for local image..."
cat > deploy/.env << 'ENVFILE'
MIS_IMAGE=mis-backend:local
APP_PORT=8000
POSTGRES_USER=mis
POSTGRES_PASSWORD=mis
POSTGRES_DB=mis
ENVFILE

echo "4. Starting Docker Compose..."
cd deploy
docker compose up -d

echo "Done. Postgres and app are running. App: http://localhost:8000"
