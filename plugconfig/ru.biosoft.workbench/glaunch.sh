#!/bin/bash

killchildren ()
{
    PARENT=$1
    for CHILD in `ps h -o pid --ppid $PARENT`
    do
		killchildren $CHILD
		kill $CHILD 2>/dev/null
    done
}

trap "killchildren $$" SIGTERM SIGINT

echo 0 > "/proc/self/oom_score_adj";

bash -c "$*" </proc/self/fd/0 &

wait $!
