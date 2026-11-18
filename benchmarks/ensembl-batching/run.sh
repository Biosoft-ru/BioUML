#!/bin/bash
# Usage: OLD=<old classes> NEW=<new classes> DEPS=<classpath> JDBC=<connector jar> ./run.sh <port> <charset> <collation>
set -e
cd "$(dirname "$0")"
PORT=${1:-3307}; CHARSET=${2:-latin1}; COLLATION=${3:-latin1_swedish_ci}
: "${OLD:?set OLD to the old classes}" "${NEW:?set NEW to the new classes}" "${DEPS:?set DEPS}" "${JDBC:?set JDBC}"
mkdir -p out results
javac -nowarn -d out -cp "$OLD:$DEPS:$JDBC" src/ensbench/EnsBench.java
P="-Djava.awt.headless=true -Dens.port=$PORT -Dens.db=ens_$COLLATION -Dens.charset=$CHARSET -Dens.collation=$COLLATION"
R=results/$COLLATION
java $P -cp "out:$OLD:$DEPS:$JDBC" ensbench.EnsBench load
java $P -cp "out:$OLD:$DEPS:$JDBC" ensbench.EnsBench verify > $R.old.txt
java $P -cp "out:$NEW:$OLD:$DEPS:$JDBC" ensbench.EnsBench verify > $R.new.txt
if cmp -s $R.old.txt $R.new.txt; then echo "verify: IDENTICAL ($(wc -l < $R.old.txt) lines)"; else echo "verify: DIFFERENT"; diff $R.old.txt $R.new.txt | head -20; exit 1; fi
echo "--- old"; java $P -cp "out:$OLD:$DEPS:$JDBC" ensbench.EnsBench bench 2>&1 >/dev/null | grep statements=
echo "--- new"; java $P -cp "out:$NEW:$OLD:$DEPS:$JDBC" ensbench.EnsBench bench 2>&1 >/dev/null | grep statements=
