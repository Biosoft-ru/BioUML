# JVode VectorUtils — JMH benchmarks

An A/B microbenchmark for commit `d6308847` ("perf(profiler): unroll remaining VectorUtils hot loops in JVode solver").

It answers three questions:

1. **Does the unroll-by-4 make each loop faster** on the JDK and CPU the solver runs on, at the vector lengths BioUML models have?
2. **Is the unrolled code really bit-identical** to the original, including aliased calls and special values?
3. **What would a multi-accumulator `wrmsNorm` buy**, and what would it cost in rounding? (Not part of the patch; it is the change that could actually speed up that reduction.)

## What's inside

| File | Purpose |
|---|---|
| `Scalar.java` | The ten VectorUtils loops before the patch, copied verbatim (baseline). |
| `Unrolled.java` | The same loops after the patch, copied verbatim (candidate). |
| `MultiAccumulator.java` | `wrmsNorm` with 2 and 4 independent partial sums. Not bit-identical. |
| `VectorUtilsBenchmark.java` | 22 benchmarks named `<operation>_<variant>`, each at n = 8, 32, 128, 512, 2048. |
| `Verify.java` | Bit-identity check (Unrolled vs Scalar) and a `wrmsNorm` accuracy table. |
| `summarize.py` | Turns JMH's JSON output into a Markdown table with a verdict per pair. |
| `results-cloud-vm/` | One indicative run on a cloud VM with JDK 21 (not the BioUML server), with raw JSON. |

Benchmark operation names map to VectorUtils methods as follows:

| Benchmark operation | VectorUtils method | Computes |
|---|---|---|
| `wrmsNorm` | `wrmsNorm(x, w)` | sqrt(Σ(x·w)² / n) |
| `scaleCopy` | `scale(c, x, z)` | z = c·x |
| `scaleInPlace` | `scale(a, x)` | x *= a |
| `linearDiff` | `linearDiff(x, y, z)` | z = x − y |
| `linearDiffScaled` | `linearDiff(a, x, y, z)` | z = a·x − y |
| `linearSumAxPlusY` | `linearSum(a, x, y, z)` | z = a·x + y |
| `linearSumXPlusBy` | `linearSum(x, b, y, z)` | z = x + b·y |
| `linearSumAddScaled` | `linearSum(a, x, z)` | z += a·x |
| `substract` | `substract(x, z)` | z −= x |
| `scaleDiff` | `scaleDiff(a, x, y, z)` | z = a·(x − y) |

Variants: `scalar` (pre-patch), `unrolled` (patch), and for `wrmsNorm` also `acc2` / `acc4`.

## Build

```bash
mvn -q clean package          # produces target/benchmarks.jar
```

Needs JDK 11 or newer and access to Maven Central. The harness is standalone; it does not touch the BioUML build.

## Run

**Where:** on the server that runs simulations (or identical hardware), with the same JDK. Results depend heavily on both: C2's auto-vectorizer has changed a lot between JDK 17, 21 and 25, and AVX2 and AVX-512 machines vectorize differently. Use a quiet machine with no other load.

**1. Check correctness first**

```bash
java -cp target/benchmarks.jar biouml.plugins.simulation.ode.jvode.jmh.Verify
```

Exits with status 1 if any unrolled method differs from its original.

**2. Benchmark**

```bash
# Full run: 110 benchmark/size combinations, 2 forks each. About 30 minutes.
java -jar target/benchmarks.jar -rf json -rff results.json

# Quick look, about 5 minutes. Noisier; use it to decide what to run in full.
java -jar target/benchmarks.jar -f 1 -wi 2 -w 500ms -i 3 -r 500ms -rf json -rff quick.json

# Only some operations, at your own model sizes (n = number of ODE variables)
java -jar target/benchmarks.jar 'wrmsNorm|linearDiffScaled' -p n=24,150 -rf json -rff subset.json
```

The first argument is a regular expression over benchmark names. `-p n=...` replaces the default sizes.

**3. Summarize**

```bash
python3 summarize.py results.json > results.md
```

## Reading the results

For every operation and size, `summarize.py` prints the baseline time, the variant time, the change (negative means the variant is faster) and the baseline's cost per element. It ends with one line per variant: the geometric mean of variant/baseline over all pairs, and how many pairs were faster, slower or indistinguishable.

A pair is called **faster** or **slower** only when JMH's two 99.9% confidence intervals do not overlap. Otherwise it is **no clear difference**. This is deliberately conservative: a win smaller than the benchmark's own noise is not a reason to carry extra code.

A reasonable merge rule: keep the unrolled version of a method only if it is clearly faster at the sizes your models use and not slower at any size.

Then confirm end to end. A microbenchmark cannot show whether these loops matter for total simulation time. Time a few representative models with the old and new build: same inputs, a warmed-up JVM and repeated runs. Only that number belongs in the commit message.

## Diagnostics

**Is C2 already vectorizing the original loops?** Run the baseline with HotSpot's auto-vectorizer (SuperWord) on and off:

```bash
java -jar target/benchmarks.jar '_scalar' -p n=512 -rf json -rff sw-on.json
java -jar target/benchmarks.jar '_scalar' -p n=512 -jvmArgsAppend -XX:-UseSuperWord -rf json -rff sw-off.json
python3 summarize.py sw-on.json --against sw-off.json
```

If the scalar loops get much slower with SuperWord off, C2 was turning them into SIMD code. `wrmsNorm` should barely change: C2 does not vectorize a strictly ordered floating-point sum.

**What code did C2 generate?** Add `-prof perfnorm` for instructions and cycles per operation, or `-prof perfasm` for the hot assembly. Both need Linux `perf`; `perfasm` also needs the `hsdis` disassembler library.

## The `wrmsNorm` accuracy table

`Verify` prints a table for `scalar`, `acc2` and `acc4` at each size, on two data sets:

- **uniform:** all terms of similar size, as in the benchmark.
- **spread:** terms spread over eight decades, closer to what JVode feeds `wrmsNorm` (corrections times error weights).

The columns are:

- **max / mean err ulp:** distance from an exact result, in units in the last place. The exact result uses the same double-rounded products x·w, then squares and sums them without rounding, and rounds sqrt(sum/n) once.
- **differs/scalar:** how often the variant's result is not bit-identical to the current scalar loop.
- **max diff ulp:** the largest such difference.

Expect the multi-accumulator variants to differ from the scalar loop most of the time, by a few ULPs at small n. Expect them to be at least as close to the exact value, because each partial sum accumulates fewer terms.

A result that differs by k ULPs can change one of JVode's accept/reject decisions only when the norm lies within about k × 2.2·10⁻¹⁶ (relative) of the threshold it is compared against. Whether that is acceptable is a policy decision for the team; this table gives the numbers for it.

## Caveats

- **The copies must match VectorUtils.** `Scalar.java` and `Unrolled.java` are copies, not the real class. If VectorUtils changes, update both.
- **`wrmsNorm`'s return statement is assumed.** It is outside the patch's diff context. It is taken to be `Math.sqrt( sum / n )`, which is what `VectorUtilsOptimizationTest` expects.
- **NaN results.** `Verify` treats all NaNs as equal. Java does not specify which NaN bit pattern arithmetic produces: when two NaN operands meet, which one propagates depends on the operand order the JIT emits. In testing, the original and unrolled `wrmsNorm` did return NaNs with different raw bits for the same input. Any NaN is still NaN to every comparison JVode makes, so this has no effect on the solver. It only means "bit-identical" holds for every non-NaN result.
- **Timing data is benign.** It contains no subnormals, which would dominate the timing on most CPUs. In-place operations are arranged so repeated calls cannot overflow: `scaleInPlace` multiplies by −1, and `z += a·x` / `z −= x` drift only linearly.
