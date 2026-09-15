#!/usr/bin/env bash
# fetch_profile.sh — Fetch profiler data from a BioUML server.
# Reads MONITORING_USER and MONITORING_PASS from .env at the current working directory.
#
# Usage:
#   # List recent profiles (JSON output, filtered by age and size)
#   bash fetch_profile.sh list <server_url>
#
#   # Get raw profile content by ID
#   bash fetch_profile.sh get <profile_id> <server_url>
#
#   # Get profile summary (legacy, single latest profile)
#   bash fetch_profile.sh summary <server_url>
#
# Server URL can also be passed via PROFILE_SERVER_URL env var:
#   export PROFILE_SERVER_URL=https://ict.biouml.org
#   bash fetch_profile.sh list
#
# List action filters: last 24 hours, size > 1000 bytes.
# Output format: JSON array of profile objects (list) or raw text (get/summary).

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

# Server URL: last positional argument, fall back to PROFILE_SERVER_URL env var.
# Capture before any shift: .env is sourced with `set -a` which exports
# PROFILE_SERVER_URL, so an unguarded `shift` would consume it from $@.
SERVER_URL="${PROFILE_SERVER_URL:-}"
for _arg in "$@"; do
  SERVER_URL="$_arg"
done
SERVER_URL="${SERVER_URL%/}"

if [[ -z "$SERVER_URL" ]]; then
  echo "ERROR: server URL is required" >&2
  echo "Usage: $0 <action> <server_url>" >&2
  echo "  or:   PROFILE_SERVER_URL=<url> $0 <action>" >&2
  exit 1
fi

ACTION="${1:-summary}"
shift

case "$ACTION" in
  list)
    # List recent profiler reports: last 24 hours, size > 1000 bytes
    LIST_URL="${SERVER_URL}/biouml/support/profile?action=list&user=${MONITORING_USER}&pass=${MONITORING_PASS}"
    echo "Fetching profile list from: ${LIST_URL}" >&2
    echo "" >&2

    curl -s -L --max-time 120 "$LIST_URL" | python3 -c "
import sys, json, time

try:
    data = json.load(sys.stdin)
except json.JSONDecodeError:
    print('ERROR: invalid JSON response', file=sys.stderr)
    sys.exit(1)

# Handle wrapper formats
if isinstance(data, dict):
    if 'value' in data and isinstance(data['value'], list):
        profiles = data['value']
    elif 'profiles' in data:
        profiles = data['profiles']
    else:
        # Assume top-level dict is a single profile entry
        profiles = [data]
elif isinstance(data, list):
    profiles = data
else:
    print('ERROR: unexpected response format', file=sys.stderr)
    sys.exit(1)

now = time.time()
cutoff = now - 86400  # 24 hours
min_size = 1000

filtered = []
for p in profiles:
    ts = p.get('timestamp', 0)
    sz = p.get('size', 0)
    if ts >= cutoff and sz > min_size:
        filtered.append(p)

print(json.dumps(filtered, indent=2))
"
    ;;

  get)
    # Get raw profile content by ID (base64-decoded)
    if [[ $# -lt 1 ]]; then
      echo "ERROR: profile ID required for 'get' action" >&2
      echo "Usage: $0 get <profile_id> <server_url>" >&2
      exit 1
    fi
    PROFILE_ID="$1"
    # PROFILE_SERVER_URL is already captured into SERVER_URL above; do not
    # shift again (it would shift the exported env var out of $@).
    GET_URL="${SERVER_URL}/biouml/support/profile?action=get&id=${PROFILE_ID}&user=${MONITORING_USER}&pass=${MONITORING_PASS}"
    echo "Fetching profile '${PROFILE_ID}' from: ${GET_URL}" >&2
    echo "" >&2

    curl -s -L --max-time 120 "$GET_URL" | python3 -c "
import sys, json, base64

try:
    data = json.load(sys.stdin)
except json.JSONDecodeError:
    # Maybe plain text response
    sys.stdout.write(sys.stdin.read())
    sys.exit(0)

# Handle JSON wrapper: extract base64 content
if isinstance(data, dict):
    if 'value' in data and isinstance(data['value'], dict):
        b64 = data['value'].get('content', '')
    elif 'content' in data:
        b64 = data['content']
    elif 'data' in data:
        b64 = data['data']
    else:
        # Try to find any base64-like field
        for key in ('content', 'data', 'value'):
            if key in data and isinstance(data[key], str):
                b64 = data[key]
                break
        else:
            print(json.dumps(data, indent=2))
            sys.exit(0)
else:
    print(json.dumps(data, indent=2))
    sys.exit(0)

# Decode base64 and print
try:
    raw = base64.b64decode(b64)
    sys.stdout.buffer.write(raw)
except Exception:
    # If not valid base64, print as-is
    sys.stdout.write(str(b64))
"
    ;;

  summary)
    # Legacy: fetch summary of latest profile
    QUERY_URL="${SERVER_URL}/biouml/support/profile?action=summary&id=latest&user=${MONITORING_USER}&pass=${MONITORING_PASS}"
    echo "Fetching profile summary from: ${QUERY_URL}" >&2
    echo "" >&2

    RESPONSE=$(curl -s -L --max-time 120 \
      -H "Accept: text/plain" \
      "$QUERY_URL")

    # Handle JSON response (API may return {"type":"ok","value":{"content":"..."}})
    if echo "$RESPONSE" | grep -q '"type"'; then
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
    ;;

  subprocess)
    # Fetch the persistent sub-process observation log (action=subProcessLog).
    # Optional extra args before the server URL: --slow-only, --since <ms>, --until <ms>
    # Prints a human-readable table of long-running external processes (perl/R/nextflow/...)
    # recorded over time — the complement to the CPU profile, which only sees the JVM.
    SP_EXTRA=""
    SLOW_ONLY=""
    SINCE=""
    UNTIL=""
    while [[ $# -gt 1 ]]; do
      case "$1" in
        --slow-only) SLOW_ONLY="&slowOnly=true"; shift ;;
        --since)     SINCE="$2"; shift 2 ;;
        --until)     UNTIL="$2"; shift 2 ;;
        *)           shift ;;
      esac
    done
    # Rebuild the query string, then drop the server URL from $@ handling.
    SERVER_URL="${PROFILE_SERVER_URL:-${1:-}}"
    [[ -n "$SINCE" ]] && SINCE="&since=${SINCE}"
    [[ -n "$UNTIL" ]] && UNTIL="&until=${UNTIL}"
    SUB_URL="${SERVER_URL%/}/biouml/support/profile?action=subProcessLog&user=${MONITORING_USER}&pass=${MONITORING_PASS}${SLOW_ONLY}${SINCE}${UNTIL}"
    echo "Fetching sub-process log from: ${SUB_URL}" >&2
    echo "" >&2

    curl -s -L --max-time 120 "$SUB_URL" | python3 -c "
import sys, json
# Read the whole response first, then parse — json.load(sys.stdin) would consume
# the stream, so a failed parse leaves nothing to echo back.
raw = sys.stdin.read()
try:
    data = json.loads(raw)
except json.JSONDecodeError:
    sys.stdout.write(raw)
    sys.exit(1)

def get_value(d):
    if isinstance(d, dict):
        if 'value' in d and isinstance(d['value'], dict):
            return d['value']
        return d
    return {}

v = get_value(data)
records = v.get('records', [])
path = v.get('path', '')
count = v.get('count', len(records))
print(f'=== Sub-process log ({count} records)  [{path}] ===')
if not records:
    print('  (no sub-processes recorded — work was in-JVM, or log is empty)')
    sys.exit(0)
import datetime
for rec in records:
    ts = rec.get('timestamp', 0)
    when = datetime.datetime.fromtimestamp(ts/1000.0).strftime('%Y-%m-%d %H:%M:%S') if ts else '?'
    subs = rec.get('subProcesses', [])
    for sp in subs:
        flag = 'SLOW ' if sp.get('slow') else ''
        print(f\"{when}  {flag}pid={sp.get('pid')} age={sp.get('ageSeconds')}s  {sp.get('command')}\")
"
    ;;

  *)
    echo "ERROR: unknown action '${ACTION}'. Use: list, get, summary, subprocess" >&2
    exit 1
    ;;
esac
