#!/usr/bin/env bash
# Starts WireMock 2.35.0 in Docker on port 8080 (stubs are registered by the tests themselves).
set -e
docker rm -f wiremock >/dev/null 2>&1 || true
docker run --rm -d \
  --name wiremock \
  -p 8080:8080 \
  -v "${PWD}/mocks:/home/wiremock" \
  wiremock/wiremock:2.35.0
echo "WireMock started on http://localhost:8080"
