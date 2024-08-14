#!/bin/sh
#set +v

rm dpi.min.js 2>/dev/null
rm dpi.js 2>/dev/null
while read p; do
  cat ../../../../../../../../../../"$p" >>dpi.js
done <dpi.buildpaths.txt
mv dpi.js dpi.min.js

