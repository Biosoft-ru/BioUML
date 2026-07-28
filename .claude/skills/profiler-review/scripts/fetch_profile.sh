#!/usr/bin/env bash
# fetch_profile.sh — Fetch the latest profiler summary from a BioUML server.
# Reads MONITORING_USER and MONITORING_PASS from .env at the current working directory.
# Output: profile summary text to stdout.
#
# Usage: run from the repo root (or set MONITORING_USER/PASS env vars)
#   bash .claude/skills/profiler-review/scripts/fetch_profile.sh https://biouml2test.biouml.org

set -euo pipefail

# Load .env from current directory if present
if [[ -f ".env" ]]; then
  set -a
  source ".env"
  set +a
fi

if [[ -z "${MONITORING_USER:-}" || -z "${MONITORING_PASS:-}" ]]; then
  echo "ERROR: MONITORING_USER and MONITORING_PASS must be set (e.g., in .env file)" >&2
  exit 1
fi

# Server URL: first argument, or default to test server
SERVER_URL="${1:-https://biouml2test.biouml.org}"
# Strip trailing slash if present
SERVER_URL="${SERVER_URL%/}"

QUERY_URL="${SERVER_URL}/biouml/support/profile?action=summary&id=latest&user=${MONITORING_USER}&pass=${MONITORING_PASS}"

echo "Fetching profile from: ${QUERY_URL}"
echo ""

RESPONSE=$(curl -s -L --max-time 120 \
  -H "Accept: text/plain" \
  "$QUERY_URL")

# Handle JSON response (API may return {"type":"ok","value":{"content":"..."}})
if echo "$RESPONSE" | grep -q '"type"'; then
  # Extract the content field from JSON using python (more reliable than jq)
  CONTENT=$(echo "$RESPONSE" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if 'value' in data and 'content' in data['value']:
        print(data['value']['content'], end='')
    elif 'content' in data:
        print(data['content'], end='')
    else:
        print(json.dumps(data, indent=2), end='')
except:
    print('$RESPONSE', end='')
" 2>/dev/null || echo "$RESPONSE")
  echo "$CONTENT"
else
  echo "$RESPONSE"
fi

echo ""
echo "--- Fetch complete ---"
