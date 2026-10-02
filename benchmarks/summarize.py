#!/usr/bin/env python3
"""Summarize JMH results from VectorUtilsBenchmark as a Markdown table.

Two modes:

  One run, variants vs. the scalar baseline (the A/B answer for the patch):
      python3 summarize.py results.json

  Two runs, same benchmarks compared across runs (e.g. SuperWord on vs. off,
  or JDK 17 vs. JDK 21):
      python3 summarize.py baseline.json --against other.json

A difference is reported as "faster" or "slower" only when the two 99.9%
confidence intervals JMH reports do not overlap; otherwise "no clear
difference". That is deliberately conservative: a microbenchmark win smaller
than its own noise is not a reason to merge code.

Requires only the Python 3 standard library.
"""

import argparse
import json
import math
import os
import sys
from collections import defaultdict

BASELINE = "scalar"


def load(path):
    """Return {(operation, variant, n): (score, error, unit)}."""
    with open(path) as f:
        runs = json.load(f)
    out = {}
    for r in runs:
        method = r["benchmark"].rsplit(".", 1)[-1]
        op, sep, variant = method.partition("_")
        if not sep:
            continue
        n = int(r.get("params", {}).get("n", 0))
        pm = r["primaryMetric"]
        try:
            err = float(pm.get("scoreError"))
        except (TypeError, ValueError):
            err = float("nan")
        out[(op, variant, n)] = (float(pm["score"]), err, pm.get("scoreUnit", ""))
    if not out:
        sys.exit(f"{path}: no VectorUtilsBenchmark results found")
    return out


def verdict(base, cand):
    (s0, e0), (s1, e1) = base[:2], cand[:2]
    if math.isnan(e0) or math.isnan(e1):
        return "n/a (no error estimate: use >= 3 measurement iterations)"
    if s1 + e1 < s0 - e0:
        return "**faster**"
    if s1 - e1 > s0 + e0:
        return "**slower**"
    return "no clear difference"


def fmt(score, err):
    if math.isnan(err):
        return f"{score:,.2f}"
    return f"{score:,.2f} ± {err:,.2f}"


def delta(s0, s1):
    return f"{(s1 / s0 - 1) * 100:+.1f}%"


def against_baseline(results):
    unit = next(iter(results.values()))[2]
    ops = sorted({k[0] for k in results})
    print(f"Times in {unit}; Δ time < 0 means the variant is faster than `{BASELINE}`.\n")
    print(f"| operation | n | {BASELINE} | variant | variant time | Δ time | ns/element ({BASELINE}) | verdict |")
    print("|---|---:|---:|---|---:|---:|---:|---|")

    ratios = defaultdict(list)
    tally = defaultdict(lambda: defaultdict(int))
    for op in ops:
        sizes = sorted({k[2] for k in results if k[0] == op})
        variants = sorted({k[1] for k in results if k[0] == op and k[1] != BASELINE})
        for n in sizes:
            base = results.get((op, BASELINE, n))
            if base is None:
                continue
            for v in variants:
                cand = results.get((op, v, n))
                if cand is None:
                    continue
                vd = verdict(base, cand)
                ratios[v].append(cand[0] / base[0])
                tally[v][vd] += 1
                per_elem = f"{base[0] / n:.3f}" if n else "–"
                print(f"| {op} | {n} | {fmt(*base[:2])} | {v} | {fmt(*cand[:2])} | "
                      f"{delta(base[0], cand[0])} | {per_elem} | {vd} |")

    print("\n**Overall, per variant** (geometric mean of variant time / scalar time over every operation and n):\n")
    print("| variant | pairs | geo-mean time ratio | faster | slower | no clear difference | no error estimate |")
    print("|---|---:|---:|---:|---:|---:|---:|")
    for v in sorted(ratios):
        g = math.exp(sum(math.log(r) for r in ratios[v]) / len(ratios[v]))
        t = tally[v]
        unknown = len(ratios[v]) - t["**faster**"] - t["**slower**"] - t["no clear difference"]
        print(f"| {v} | {len(ratios[v])} | {g:.3f} | {t['**faster**']} | {t['**slower**']} | "
              f"{t['no clear difference']} | {unknown} |")


def across_runs(base_results, other_results, base_name, other_name):
    unit = next(iter(base_results.values()))[2]
    keys = sorted(set(base_results) & set(other_results))
    if not keys:
        sys.exit("The two files have no benchmarks in common.")
    print(f"Times in {unit}; Δ time < 0 means `{other_name}` is faster than `{base_name}`.\n")
    print(f"| benchmark | n | {base_name} | {other_name} | Δ time | verdict |")
    print("|---|---:|---:|---:|---:|---|")
    for op, v, n in keys:
        b, o = base_results[(op, v, n)], other_results[(op, v, n)]
        print(f"| {op}_{v} | {n} | {fmt(*b[:2])} | {fmt(*o[:2])} | {delta(b[0], o[0])} | {verdict(b, o)} |")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("results", help="JMH JSON output (-rf json -rff FILE)")
    ap.add_argument("--against", metavar="OTHER", help="second JMH JSON run to compare benchmark-by-benchmark")
    args = ap.parse_args()

    base = load(args.results)
    if args.against:
        across_runs(base, load(args.against), os.path.basename(args.results), os.path.basename(args.against))
    else:
        against_baseline(base)


if __name__ == "__main__":
    main()
