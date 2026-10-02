# Indicative results: cloud VM, JDK 21

**Not the BioUML server.** Re-run on the server's hardware and JDK before deciding anything; see `../README.md`.

- Machine: shared cloud VM, 2 vCPUs, Intel Xeon @ 2.80 GHz with AVX-512 (`UseAVX=3`, `MaxVectorSize=64`)
- JDK: OpenJDK 21.0.10 (Ubuntu 24.04), default flags
- JMH 1.37, shorter settings than the harness defaults: `-f 2 -wi 2 -w 500ms -i 4 -r 500ms`. Some confidence intervals are wide as a result.
- Raw JMH output: `main.json`, `nosuperword.json`, `nosuperword-unrolled.json`

## 1. Patch A/B: unrolled vs. original loop

`python3 summarize.py main.json`

Times in ns/op; Δ time < 0 means the variant is faster than `scalar`.

| operation | n | scalar | variant | variant time | Δ time | ns/element (scalar) | verdict |
|---|---:|---:|---|---:|---:|---:|---|
| linearDiff | 8 | 8.93 ± 2.16 | unrolled | 8.28 ± 1.99 | -7.2% | 1.116 | no clear difference |
| linearDiff | 32 | 11.76 ± 2.78 | unrolled | 25.06 ± 4.05 | +113.0% | 0.368 | **slower** |
| linearDiff | 128 | 23.14 ± 3.02 | unrolled | 100.77 ± 22.09 | +335.6% | 0.181 | **slower** |
| linearDiff | 512 | 90.05 ± 9.86 | unrolled | 421.63 ± 131.48 | +368.2% | 0.176 | **slower** |
| linearDiff | 2048 | 753.83 ± 59.92 | unrolled | 1,666.11 ± 343.82 | +121.0% | 0.368 | **slower** |
| linearDiffScaled | 8 | 10.39 ± 5.26 | unrolled | 8.36 ± 1.00 | -19.5% | 1.298 | no clear difference |
| linearDiffScaled | 32 | 11.22 ± 1.64 | unrolled | 30.89 ± 10.46 | +175.4% | 0.351 | **slower** |
| linearDiffScaled | 128 | 24.31 ± 4.60 | unrolled | 98.72 ± 21.15 | +306.1% | 0.190 | **slower** |
| linearDiffScaled | 512 | 96.12 ± 6.08 | unrolled | 454.49 ± 117.56 | +372.8% | 0.188 | **slower** |
| linearDiffScaled | 2048 | 877.99 ± 203.01 | unrolled | 2,482.96 ± 1,502.61 | +182.8% | 0.429 | no clear difference |
| linearSumAddScaled | 8 | 7.69 ± 1.32 | unrolled | 6.59 ± 1.35 | -14.3% | 0.962 | no clear difference |
| linearSumAddScaled | 32 | 9.31 ± 0.92 | unrolled | 23.35 ± 8.55 | +150.9% | 0.291 | **slower** |
| linearSumAddScaled | 128 | 23.82 ± 4.71 | unrolled | 84.56 ± 16.44 | +255.0% | 0.186 | **slower** |
| linearSumAddScaled | 512 | 71.40 ± 6.06 | unrolled | 352.75 ± 55.24 | +394.0% | 0.139 | **slower** |
| linearSumAddScaled | 2048 | 753.52 ± 44.49 | unrolled | 1,532.72 ± 167.55 | +103.4% | 0.368 | **slower** |
| linearSumAxPlusY | 8 | 7.17 ± 0.24 | unrolled | 7.90 ± 1.15 | +10.2% | 0.896 | no clear difference |
| linearSumAxPlusY | 32 | 12.01 ± 2.14 | unrolled | 28.05 ± 10.85 | +133.6% | 0.375 | **slower** |
| linearSumAxPlusY | 128 | 23.11 ± 2.53 | unrolled | 104.56 ± 21.54 | +352.5% | 0.181 | **slower** |
| linearSumAxPlusY | 512 | 94.89 ± 3.30 | unrolled | 410.68 ± 55.04 | +332.8% | 0.185 | **slower** |
| linearSumAxPlusY | 2048 | 1,781.16 ± 2,101.47 | unrolled | 1,842.16 ± 281.38 | +3.4% | 0.870 | no clear difference |
| linearSumXPlusBy | 8 | 8.79 ± 2.02 | unrolled | 7.96 ± 0.96 | -9.4% | 1.099 | no clear difference |
| linearSumXPlusBy | 32 | 11.83 ± 2.32 | unrolled | 32.64 ± 14.23 | +176.0% | 0.370 | **slower** |
| linearSumXPlusBy | 128 | 23.50 ± 1.98 | unrolled | 114.93 ± 23.44 | +389.0% | 0.184 | **slower** |
| linearSumXPlusBy | 512 | 103.81 ± 11.75 | unrolled | 470.64 ± 199.77 | +353.4% | 0.203 | **slower** |
| linearSumXPlusBy | 2048 | 794.37 ± 56.15 | unrolled | 2,057.99 ± 665.01 | +159.1% | 0.388 | **slower** |
| scaleCopy | 8 | 7.18 ± 3.02 | unrolled | 5.99 ± 2.39 | -16.6% | 0.898 | no clear difference |
| scaleCopy | 32 | 8.29 ± 0.58 | unrolled | 22.65 ± 13.58 | +173.1% | 0.259 | **slower** |
| scaleCopy | 128 | 21.30 ± 6.92 | unrolled | 71.75 ± 11.65 | +236.9% | 0.166 | **slower** |
| scaleCopy | 512 | 54.68 ± 15.01 | unrolled | 358.47 ± 186.91 | +555.6% | 0.107 | **slower** |
| scaleCopy | 2048 | 550.59 ± 84.82 | unrolled | 1,258.54 ± 151.53 | +128.6% | 0.269 | **slower** |
| scaleDiff | 8 | 8.78 ± 2.54 | unrolled | 7.42 ± 0.97 | -15.5% | 1.098 | no clear difference |
| scaleDiff | 32 | 12.10 ± 3.44 | unrolled | 30.69 ± 9.72 | +153.7% | 0.378 | **slower** |
| scaleDiff | 128 | 25.53 ± 7.36 | unrolled | 114.90 ± 17.62 | +350.1% | 0.199 | **slower** |
| scaleDiff | 512 | 156.26 ± 104.61 | unrolled | 455.23 ± 121.66 | +191.3% | 0.305 | **slower** |
| scaleDiff | 2048 | 784.60 ± 68.85 | unrolled | 1,868.75 ± 524.22 | +138.2% | 0.383 | **slower** |
| scaleInPlace | 8 | 7.62 ± 1.10 | unrolled | 4.92 ± 1.05 | -35.4% | 0.952 | **faster** |
| scaleInPlace | 32 | 11.19 ± 3.72 | unrolled | 17.09 ± 9.18 | +52.7% | 0.350 | no clear difference |
| scaleInPlace | 128 | 17.28 ± 2.96 | unrolled | 58.28 ± 8.41 | +237.3% | 0.135 | **slower** |
| scaleInPlace | 512 | 45.88 ± 15.29 | unrolled | 265.09 ± 91.47 | +477.8% | 0.090 | **slower** |
| scaleInPlace | 2048 | 165.96 ± 81.12 | unrolled | 894.36 ± 97.99 | +438.9% | 0.081 | **slower** |
| substract | 8 | 8.28 ± 1.85 | unrolled | 6.79 ± 1.87 | -18.0% | 1.035 | no clear difference |
| substract | 32 | 10.76 ± 1.49 | unrolled | 21.01 ± 3.66 | +95.3% | 0.336 | **slower** |
| substract | 128 | 23.84 ± 6.57 | unrolled | 79.11 ± 19.84 | +231.9% | 0.186 | **slower** |
| substract | 512 | 76.40 ± 13.86 | unrolled | 303.98 ± 22.38 | +297.9% | 0.149 | **slower** |
| substract | 2048 | 769.37 ± 123.66 | unrolled | 1,609.32 ± 254.11 | +109.2% | 0.376 | **slower** |
| wrmsNorm | 8 | 8.47 ± 1.69 | acc2 | 12.06 ± 1.39 | +42.3% | 1.059 | **slower** |
| wrmsNorm | 8 | 8.47 ± 1.69 | acc4 | 7.80 ± 1.09 | -8.0% | 1.059 | no clear difference |
| wrmsNorm | 8 | 8.47 ± 1.69 | unrolled | 11.62 ± 1.32 | +37.2% | 1.059 | **slower** |
| wrmsNorm | 32 | 29.58 ± 5.37 | acc2 | 34.98 ± 5.12 | +18.3% | 0.924 | no clear difference |
| wrmsNorm | 32 | 29.58 ± 5.37 | acc4 | 27.14 ± 5.95 | -8.3% | 0.924 | no clear difference |
| wrmsNorm | 32 | 29.58 ± 5.37 | unrolled | 34.29 ± 5.00 | +15.9% | 0.924 | no clear difference |
| wrmsNorm | 128 | 123.91 ± 17.34 | acc2 | 130.07 ± 11.74 | +5.0% | 0.968 | no clear difference |
| wrmsNorm | 128 | 123.91 ± 17.34 | acc4 | 94.39 ± 19.55 | -23.8% | 0.968 | no clear difference |
| wrmsNorm | 128 | 123.91 ± 17.34 | unrolled | 153.55 ± 16.19 | +23.9% | 0.968 | no clear difference |
| wrmsNorm | 512 | 627.21 ± 18.53 | acc2 | 561.01 ± 137.92 | -10.6% | 1.225 | no clear difference |
| wrmsNorm | 512 | 627.21 ± 18.53 | acc4 | 366.28 ± 67.37 | -41.6% | 1.225 | **faster** |
| wrmsNorm | 512 | 627.21 ± 18.53 | unrolled | 661.71 ± 47.90 | +5.5% | 1.225 | no clear difference |
| wrmsNorm | 2048 | 2,529.34 ± 81.31 | acc2 | 2,363.33 ± 876.59 | -6.6% | 1.235 | no clear difference |
| wrmsNorm | 2048 | 2,529.34 ± 81.31 | acc4 | 1,481.52 ± 214.39 | -41.4% | 1.235 | **faster** |
| wrmsNorm | 2048 | 2,529.34 ± 81.31 | unrolled | 2,626.25 ± 155.89 | +3.8% | 1.235 | no clear difference |

**Overall, per variant** (geometric mean of variant time / scalar time over every operation and n):

| variant | pairs | geo-mean time ratio | faster | slower | no clear difference | no error estimate |
|---|---:|---:|---:|---:|---:|---:|
| acc2 | 5 | 1.081 | 0 | 1 | 4 | 0 |
| acc4 | 5 | 0.739 | 2 | 0 | 3 | 0 |
| unrolled | 50 | 2.256 | 1 | 34 | 15 | 0 |

## 2. Original (scalar) loops with C2's auto-vectorizer on vs. off, n = 512

`-jvmArgsAppend -XX:-UseSuperWord`. A large slowdown means C2 was compiling the loop to SIMD code.

Times in ns/op; Δ time < 0 means `nosuperword.json` is faster than `main.json`.

| benchmark | n | main.json | nosuperword.json | Δ time | verdict |
|---|---:|---:|---:|---:|---|
| linearDiff_scalar | 512 | 90.05 ± 9.86 | 330.89 ± 32.76 | +267.5% | **slower** |
| linearDiffScaled_scalar | 512 | 96.12 ± 6.08 | 612.79 ± 564.41 | +537.5% | no clear difference |
| linearSumAddScaled_scalar | 512 | 71.40 ± 6.06 | 227.74 ± 61.06 | +219.0% | **slower** |
| linearSumAxPlusY_scalar | 512 | 94.89 ± 3.30 | 351.34 ± 33.06 | +270.3% | **slower** |
| linearSumXPlusBy_scalar | 512 | 103.81 ± 11.75 | 350.10 ± 40.00 | +237.3% | **slower** |
| scaleCopy_scalar | 512 | 54.68 ± 15.01 | 123.89 ± 10.26 | +126.6% | **slower** |
| scaleDiff_scalar | 512 | 156.26 ± 104.61 | 377.57 ± 72.12 | +141.6% | **slower** |
| scaleInPlace_scalar | 512 | 45.88 ± 15.29 | 142.23 ± 40.20 | +210.0% | **slower** |
| substract_scalar | 512 | 76.40 ± 13.86 | 185.79 ± 15.57 | +143.2% | **slower** |
| wrmsNorm_scalar | 512 | 627.21 ± 18.53 | 639.08 ± 28.80 | +1.9% | no clear difference |

## 3. Unrolled loops with C2's auto-vectorizer on vs. off, n = 512

No change means C2 was not vectorizing the unrolled loop in the first place.

Times in ns/op; Δ time < 0 means `nosuperword-unrolled.json` is faster than `main.json`.

| benchmark | n | main.json | nosuperword-unrolled.json | Δ time | verdict |
|---|---:|---:|---:|---:|---|
| linearDiff_unrolled | 512 | 421.63 ± 131.48 | 407.93 ± 61.47 | -3.2% | no clear difference |
| linearDiffScaled_unrolled | 512 | 454.49 ± 117.56 | 487.01 ± 220.83 | +7.2% | no clear difference |
| linearSumAddScaled_unrolled | 512 | 352.75 ± 55.24 | 336.11 ± 51.72 | -4.7% | no clear difference |
| linearSumAxPlusY_unrolled | 512 | 410.68 ± 55.04 | 390.58 ± 44.52 | -4.9% | no clear difference |
| linearSumXPlusBy_unrolled | 512 | 470.64 ± 199.77 | 438.11 ± 90.80 | -6.9% | no clear difference |
| scaleCopy_unrolled | 512 | 358.47 ± 186.91 | 285.19 ± 74.27 | -20.4% | no clear difference |
| scaleDiff_unrolled | 512 | 455.23 ± 121.66 | 411.37 ± 43.64 | -9.6% | no clear difference |
| scaleInPlace_unrolled | 512 | 265.09 ± 91.47 | 225.79 ± 72.17 | -14.8% | no clear difference |
| substract_unrolled | 512 | 303.98 ± 22.38 | 301.82 ± 50.97 | -0.7% | no clear difference |
| wrmsNorm_unrolled | 512 | 661.71 ± 47.90 | 648.80 ± 62.72 | -2.0% | no clear difference |

## 4. `Verify` output

```
1. Unrolled vs Scalar: bit-identity
   143,090 array/result comparisons over 77 lengths, 0 mismatches -> OK

2. wrmsNorm variants: error against an exact sum, and difference from the scalar result
   Errors in ULPs of the exact result. 'exact' sums the (double-rounded) products x[i]*w[i]
   squared and added without rounding, then rounds sqrt(sum/n) once.

   data         n  variant     max err ulp   mean err ulp   differs/scalar     max diff ulp
   uniform      8  scalar             1.00          0.230             0.0%              0.0
   uniform      8  acc2               1.00          0.191            23.0%              2.0
   uniform      8  acc4               1.00          0.187            22.2%              2.0
   uniform     32  scalar             2.00          0.442             0.0%              0.0
   uniform     32  acc2               2.00          0.296            42.2%              3.0
   uniform     32  acc4               1.00          0.240            42.8%              2.0
   uniform    128  scalar             3.00          0.810             0.0%              0.0
   uniform    128  acc2               2.00          0.475            67.0%              4.0
   uniform    128  acc4               2.00          0.304            62.4%              4.0
   uniform    512  scalar             7.00          1.806             0.0%              0.0
   uniform    512  acc2               4.00          0.894            82.5%              9.0
   uniform    512  acc4               2.00          0.515            81.2%              8.0
   uniform   2048  scalar            14.00          3.635             0.0%              0.0
   uniform   2048  acc2               7.00          1.784            92.1%             17.0
   uniform   2048  acc4               4.00          0.856            89.4%             14.0
   spread       8  scalar             1.00          0.234             0.0%              0.0
   spread       8  acc2               1.00          0.219            20.1%              1.0
   spread       8  acc4               1.00          0.216            23.1%              2.0
   spread      32  scalar             3.00          0.447             0.0%              0.0
   spread      32  acc2               2.00          0.338            35.0%              3.0
   spread      32  acc4               2.00          0.287            41.8%              3.0
   spread     128  scalar             4.00          0.820             0.0%              0.0
   spread     128  acc2               4.00          0.532            58.9%              4.0
   spread     128  acc4               3.00          0.385            61.0%              5.0
   spread     512  scalar             8.00          1.589             0.0%              0.0
   spread     512  acc2               7.00          0.925            80.2%             10.0
   spread     512  acc4               3.00          0.541            80.3%              9.0
   spread    2048  scalar            15.00          2.986             0.0%              0.0
   spread    2048  acc2              10.00          1.525            88.5%             21.0
   spread    2048  acc4               4.00          0.828            87.1%             15.0
```
