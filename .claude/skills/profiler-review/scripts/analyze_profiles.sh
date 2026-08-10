#!/usr/bin/env bash
# Fetch and analyze top hot functions from a collapsed profile
set -euo pipefail

PROFILE_ID="$1"
SERVER_URL="$2"
OUTPUT_DIR="$3"

# Load .env
if [[ -f ".env" ]]; then
  set -a
  source ".env"
  set +a
fi

# Fetch the profile
PROFILE_FILE="${OUTPUT_DIR}/$(basename "${PROFILE_ID}")"
curl -s -L --max-time 120 \
  "${SERVER_URL}/biouml/support/profile?action=get&id=${PROFILE_ID}&user=${MONITORING_USER}&pass=${MONITORING_PASS}" \
  > "${PROFILE_FILE}.raw"

# The API returns base64-encoded content. Decode it.
python3 -c "
import sys, json, base64
data = json.load(sys.stdin)
if isinstance(data, dict):
    if 'value' in data and isinstance(data['value'], dict):
        b64 = data['value'].get('content', '')
    elif 'content' in data:
        b64 = data['content']
    elif 'data' in data:
        b64 = data['data']
    else:
        for key in ('content', 'data', 'value'):
            if key in data and isinstance(data[key], str):
                b64 = data[key]
                break
        else:
            print('ERROR: no content field found', file=sys.stderr)
            sys.exit(1)
else:
    print('ERROR: unexpected format', file=sys.stderr)
    sys.exit(1)
raw = base64.b64decode(b64)
sys.stdout.buffer.write(raw)
" < "${PROFILE_FILE}.raw" > "${PROFILE_FILE}"
rm -f "${PROFILE_FILE}.raw"

# Extract top functions (first 200 lines of collapsed stacks)
head -200 "${PROFILE_FILE}" > "${OUTPUT_DIR}/$(basename "${PROFILE_ID}" .collapsed).top200.txt"

# Extract unique function names and their sample counts
python3 -c "
import sys
from collections import Counter

func_counts = Counter()
total_samples = 0

with open(sys.argv[1]) as f:
    for line in f:
        line = line.strip()
        if not line:
            continue
        # Format: 'samples count; func1; func2; ...'
        parts = line.split(';')
        try:
            samples = int(parts[0].strip())
        except (ValueError, IndexError):
            continue
        total_samples += samples
        for p in parts[1:]:
            func = p.strip()
            if func:
                func_counts[func] += 1

# Top 50 functions by total appearances
print(f'Total samples: {total_samples}')
print(f'Unique functions: {len(func_counts)}')
print()
print('Top 50 functions by sample count:')
for func, count in func_counts.most_common(50):
    print(f'  {count:>8}  {func}')
" "${PROFILE_FILE}" > "${OUTPUT_DIR}/$(basename "${PROFILE_ID}" .collapsed).summary.txt"
